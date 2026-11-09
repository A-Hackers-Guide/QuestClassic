//! QuestClassic Companion daemon — M0 skeleton.
//!
//! This binary currently only logs that it started. The transport layer,
//! display drivers, input injectors and WebRTC pipeline each land in their
//! own milestone PR — see `companion/ROADMAP.md`.

use questclassic_protocol::{DEFAULT_PORT, DEFAULT_PROTOCOL_VERSION};

fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()),
        )
        .init();

    tracing::info!(
        version = env!("CARGO_PKG_VERSION"),
        protocol = DEFAULT_PROTOCOL_VERSION,
        port = DEFAULT_PORT,
        "QuestClassic Companion starting (skeleton build)"
    );
    tracing::info!("Transport, drivers and WebRTC are not wired yet — see ROADMAP.md.");
    tracing::info!("Idling so launchd / systemd / Windows Service can monitor the process.");

    // Idle loop — real transport comes online in M1.
    let rt = tokio::runtime::Runtime::new()?;
    rt.block_on(async {
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(60)).await;
            tracing::debug!("companion heartbeat");
        }
    })
}
