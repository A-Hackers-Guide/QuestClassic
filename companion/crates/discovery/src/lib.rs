//! Service descriptors for the QuestClassic Companion mDNS flow.
//!
//! The headset announces itself on the LAN so the desktop can find it
//! without the user typing an IP. This crate is intentionally minimal —
//! service name, record shape, TXT-record helpers — so both the
//! desktop (that advertises) and the headset (that resolves) share one
//! source of truth for the wire shape.
//!
//! An actual mDNS backend (mdns-sd / libmdns) binds to this crate; we
//! don't pick a backend here to keep the headset-side resolver portable.

#![forbid(unsafe_code)]

use serde::{Deserialize, Serialize};

/// DNS-SD service type the companion advertises under. The underscore
/// prefix is part of the DNS-SD spec; the trailing dot is the usual
/// mDNS-browser form.
pub const SERVICE_TYPE: &str = "_questclassic-companion._tcp.local.";

/// Default TCP port the companion listens on; matches
/// `questclassic_protocol::DEFAULT_PORT`.
pub const DEFAULT_PORT: u16 = 48122;

/// TXT-record key names. Keep each key under 15 chars for broad
/// Bonjour compatibility.
pub mod txt_keys {
    pub const PROTO_VER: &str = "pv";
    pub const APP_VER: &str = "av";
    pub const DEVICE_LABEL: &str = "name";
    /// Host-advertised capabilities as a comma-separated list, e.g.
    /// "monitors,fileshare,voice". Clients ignore unknown tokens.
    pub const CAPABILITIES: &str = "cap";
}

/// What a browser sees after resolving the service.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ResolvedCompanion {
    /// Instance name the host advertised (e.g. "Alex's laptop").
    pub instance_name: String,
    pub host: String,
    pub port: u16,
    pub proto_version: u16,
    pub app_version: String,
    pub capabilities: Vec<String>,
}

impl ResolvedCompanion {
    /// Build an instance from a TXT map returned by the mDNS backend.
    ///
    /// Missing keys fall back to sensible defaults so an old companion
    /// still resolves — the caller can decide whether to connect.
    pub fn from_txt<I, K, V>(
        instance_name: impl Into<String>,
        host: impl Into<String>,
        port: u16,
        txt: I,
    ) -> Self
    where
        I: IntoIterator<Item = (K, V)>,
        K: AsRef<str>,
        V: AsRef<str>,
    {
        let mut proto_version: u16 = 0;
        let mut app_version = String::from("0.0.0");
        let mut capabilities: Vec<String> = Vec::new();
        for (k, v) in txt {
            let k = k.as_ref();
            let v = v.as_ref();
            match k {
                txt_keys::PROTO_VER => {
                    proto_version = v.parse().unwrap_or(0);
                }
                txt_keys::APP_VER => app_version = v.to_owned(),
                txt_keys::CAPABILITIES => {
                    capabilities = v
                        .split(',')
                        .map(str::trim)
                        .filter(|s| !s.is_empty())
                        .map(str::to_owned)
                        .collect();
                }
                _ => {}
            }
        }
        ResolvedCompanion {
            instance_name: instance_name.into(),
            host: host.into(),
            port,
            proto_version,
            app_version,
            capabilities,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn service_type_is_dns_sd_shape() {
        assert!(SERVICE_TYPE.starts_with('_'));
        assert!(SERVICE_TYPE.ends_with(".local."));
        assert!(SERVICE_TYPE.contains("._tcp."));
    }

    #[test]
    fn default_port_matches_protocol_crate() {
        // Keep in lock-step with questclassic_protocol::DEFAULT_PORT.
        // When they drift, bumping one requires bumping the other and
        // rebuilding both sides; nothing in this crate pulls the
        // protocol crate in so a duplicated literal is cheaper than
        // a cross-crate dep.
        assert_eq!(DEFAULT_PORT, 48122);
    }

    #[test]
    fn from_txt_parses_known_keys() {
        let r = ResolvedCompanion::from_txt(
            "Alex's laptop",
            "alex.local",
            12345,
            [
                (txt_keys::PROTO_VER, "1"),
                (txt_keys::APP_VER, "0.1.2"),
                (txt_keys::CAPABILITIES, "monitors,fileshare,voice"),
                ("unknown", "ignored"),
            ],
        );
        assert_eq!(r.proto_version, 1);
        assert_eq!(r.app_version, "0.1.2");
        assert_eq!(r.capabilities, vec!["monitors", "fileshare", "voice"]);
        assert_eq!(r.port, 12345);
        assert_eq!(r.host, "alex.local");
    }

    #[test]
    fn from_txt_tolerates_missing_keys() {
        let r = ResolvedCompanion::from_txt(
            "bare",
            "h.local",
            DEFAULT_PORT,
            std::iter::empty::<(&str, &str)>(),
        );
        assert_eq!(r.proto_version, 0);
        assert_eq!(r.app_version, "0.0.0");
        assert!(r.capabilities.is_empty());
    }

    #[test]
    fn from_txt_bad_proto_version_falls_back_to_zero() {
        let r = ResolvedCompanion::from_txt(
            "weird",
            "h.local",
            DEFAULT_PORT,
            [(txt_keys::PROTO_VER, "not-a-number")],
        );
        assert_eq!(r.proto_version, 0);
    }

    #[test]
    fn from_txt_trims_whitespace_and_drops_empty_cap_tokens() {
        let r = ResolvedCompanion::from_txt(
            "ws",
            "h.local",
            DEFAULT_PORT,
            [(txt_keys::CAPABILITIES, " monitors , ,voice,")],
        );
        assert_eq!(r.capabilities, vec!["monitors", "voice"]);
    }

    #[test]
    fn txt_keys_fit_bonjour_15_char_rule() {
        for k in [
            txt_keys::PROTO_VER,
            txt_keys::APP_VER,
            txt_keys::DEVICE_LABEL,
            txt_keys::CAPABILITIES,
        ] {
            assert!(k.len() <= 15, "key '{}' too long: {}", k, k.len());
        }
    }
}
