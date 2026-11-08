# Meta auth flow

How the headset proves its identity to Meta's backend, and how your user account is layered on top of that.

## Two layers, in parallel

**Device auth** — proves "this is a real Meta Quest 2 with factory keybox N."
**User auth** — proves "this is user X signing in with their Meta account."

Every request to Meta's graph carries BOTH tokens. The backend can reject either independently.

## Layer 1: Device Auth (DeviceAuthServer, `com.oculus.deviceauthserver`)

### Who holds the private key

Qualcomm's **Keybox**, installed at manufacture by `vendor.oculus.hardware.keyboxinstaller@1.0-service`. The private key lives in TrustZone-protected secure storage — Android userspace can never read it. `DeviceAuth` talks to it over RPC via:

- `oculus.internal.cms.ICMHeadsetRpc` (`cms_hmd_rpc`) — headset side
- `oculus.internal.cms.ICMControllerRpc` (`cms_ctrl_rpc`) — controllers

"CMS" = Credential Management System, the TrustZone trusted-app that owns the key.

### Challenge/response protocol

1. `DeviceChallengeFetcher.fetchChallenge()`:
   ```
   POST https://graph.facebook-hardware.com/…
   { auth_protocol_version: 1,
     access_token: DEVICE_AUTH_SERVICE_ACCESS_TOKEN,
     device_certificate: <pem>,
     supported_algorithms: [SHA256withRSA, SHA384withRSA, SHA512withRSA] }
   → { challenge, signature_algorithm }
   ```
2. `DeviceAuth` hands the challenge bytes to CMS via `ICMHeadsetRpc`. Signing happens inside QSEE. The signature comes back to Android userspace.
3. Login with the signed challenge:
   ```
   POST https://graph.facebook-hardware.com/…
   LoginChallenge { device_id, device_type, nonce, signature }
   → { device_access_token, expiration_time_absolute }
   ```

### Caching + refresh

- Tokens cached in `DeviceAuthStore` (device-protected storage, available before user unlock).
- `DeviceAccessTokenExtendJobService` refreshes 10 min before expiry.
- On refresh: broadcasts `com.oculus.deviceauthservice.NEW_TOKEN_AVAILABLE`; every client rereads.

### Error codes

- `HARDWARE_AUTH__DEVICE_NOT_REGISTERED = 2699001` — re-register the device cert.
- `HARDWARE_AUTH__REAUTH_REQUIRED = 2699012` — full challenge/response again.

### Rate limiting

HTTP 400/401/500 trip a `ConcurrentRequestThrottleQueue` on the client — client-side rate limit to avoid hammering the backend when it's complaining.

## Layer 2: User Identity (IdentityManagement, `com.oculus.identitymanagement.service`)

### Where the account state lives

A Room SQLite `CapabilitiesDatabase` with entities:

```
Account                         — Meta accounts signed in
ActiveUser                      — which one is current
Application                     — every installed Meta/3P app
Capability                      — feature flags
CapabilityTag                   — grouping
AccountCapability               — join account ↔ capability
AccountApplicationCapability    — join account ↔ app ↔ capability
```

So on-device there's a materialized `{user × app × capability}` matrix — queried before anything tries to do anything privileged.

### Broadcast-driven workers

`IdentityManagementServiceReceiver` listens for:

- `android.intent.action.BOOT_COMPLETED`
- `com.oculus.horizon.ACCESS_TOKEN_CHANGED` — key signal

On each, schedules a constellation of WorkManager jobs:

```
AccessTokenWorker                              refresh user token
UserIdentityContextWorker                      who am I right now
CapabilitiesQueryWorker                        what can the device do
UserAccountCapabilitiesQueryWorker             what can THIS user do
UserAccountApplicationGroupingCapabilitiesQueryWorker   per-app perms
SyncPreferencesStoreWorker                     sync settings
SalsaWorker                                    (unknown Meta codename)
NeutralAgeGateWorker                           age-gate / COPPA check
```

Periodic job runs every 15 min to keep it fresh.

### Hardcoded Meta-domain recognition

For OAuth callback trust:

```
facebook.com, facebook-hardware.com, facebookstudy.com,
facebookvirtualassistant.com, instagram.com
```

### Known cross-app identity package allow-list

```
FACEBOOK_HORIZON_DEV / RC / RELEASE_TEST / STABLE / STAGING
INSTAGRAM_INHOUSE / PROD / PRODV2 / PROD_K1 / PROD_SET
```

If those specific Facebook/Instagram builds are installed on the headset, they share auth state with HorizonOS. On retail they aren't — but the handshake is wired.

## The MVAN supervision layer

MVAN = Meta's supervised / teen-account enforcement system. Pieces:

- `MVanDatabase` — Room DB for MVAN state.
- `MVanServiceReceiver` — triggers.
- `MVanScreenTimeUploadWorker` — uploads screen-time usage to Meta.
- `MvanLaunchRestrictionDataQuery` — "can this user launch this app right now?" Runs before every app launch.
- `NeutralAgeGateActions` / `NeutralAgeGateWorker` — Meta's name for the age-verification flow.
- Dialog permissions: `SHOW_MVAN_LAUNCH_RESTRICTED_DIALOG`, `SHOW_MVAN_SOCIAL_RESTRICTED_DIALOG`, `SHOW_LOGIN_WITH_TWILIGHT_DIALOG_ACTIVITY`.
- "Twilight" = login-dialog flavor for supervised accounts.

Enforcement runs server-side AND cached client-side (`CapabilitiesDatabase`) — can't be bypassed offline.

## End-to-end sign-in sequence

```
0.  init → trackingservice, orchestrator, xrspd, parfait up
1.  DeviceAuthService.onCreate — DeviceAuthStore loaded (empty on factory reset)
2.  First caller requests device token
    ↓
    DeviceChallengeFetcher.fetchChallenge — POST graph.facebook-hardware.com
    ↓
    DeviceAuth hands challenge to QSEE via ICMHeadsetRpc
    ↓
    QSEE signs with factory keybox key
    ↓
    POST graph.facebook-hardware.com (LoginChallenge + signature)
    ← device_access_token + expiration
    ↓
    DeviceAuthStore cached, NEW_TOKEN_AVAILABLE broadcast, 10-min pre-expiry refresh scheduled
3.  VrShell draws FirstTimeNuxHollywood
4.  AccountsCenterPWA opens accountscenter.meta.com
    User signs in; DAT in request headers
    ↓
    (optional) CompanionServer accepts account from phone companion
5.  User access token returned
    Broadcast ACCESS_TOKEN_CHANGED
    ↓
    IdentityManagement starts workers, populates CapabilitiesDatabase
    ↓
    Periodic 15-min job scheduled
6.  NeutralAgeGateWorker runs
7.  If supervised: MVAN workers start
8.  Every graph.* request now carries both tokens
9.  Horizon Platform providers come up
10. Headset at Home
```

## Why this is unusually robust

- **Device cert in TrustZone** — rooting Android doesn't expose it.
- **User token requires device token** — can't be replayed from a non-Quest client.
- **Tokens auto-expire; can be revoked** — Meta can nuke a specific keybox.
- **Supervised restrictions enforced both server + client** — can't be bypassed offline.

That's also why there's no realistic path to using a Meta account on a Quest 2 without a working device cert — you'd have to compromise QSEE, which is the hard problem all Qualcomm-TrustZone research has been trying for years.

## See also

- `app/src/main/kotlin/io/github/questclassic/app/ui/panels/MetaHostsAuditPanel.kt` — surfaces the hosts this flow hits.
- [security/firmware-review.md § F-01..F-10](../security/firmware-review.md) — related findings.
- Parent project's `REVERSE_ENGINEERING_REPORT.md` § 19.

---

_Last reviewed against the Nov 2026 Quest 2 OTA._
