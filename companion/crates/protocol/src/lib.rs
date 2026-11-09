//! Wire protocol for QuestClassic Companion.
//!
//! One frame per websocket message, discriminated by the `op` field. See
//! `PROTOCOL.md` in the companion crate for the human-readable spec. Both
//! the companion (Rust) and the Quest app (Kotlin, via a stub generator in
//! a later milestone) treat this crate as the single source of truth.

use serde::{Deserialize, Serialize};

pub const DEFAULT_PORT: u16 = 48122;
pub const DEFAULT_PROTOCOL_VERSION: u32 = 1;

/// Top-level envelope. Every message is one of these.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(tag = "op")]
pub enum Frame {
    #[serde(rename = "HELLO")]
    Hello(Hello),
    #[serde(rename = "WELCOME")]
    Welcome(Welcome),
    #[serde(rename = "FRAME")]
    Video(VideoFrame),
    #[serde(rename = "INPUT")]
    Input(InputEvent),
    #[serde(rename = "DISPLAY_READY")]
    DisplayReady(DisplayReady),
    #[serde(rename = "DISPLAY_GONE")]
    DisplayGone(DisplayGone),
    #[serde(rename = "CURSOR")]
    Cursor(CursorEvent),
    #[serde(rename = "ERROR")]
    Error(ProtoError),
    #[serde(rename = "GOODBYE")]
    Goodbye(Goodbye),
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Hello {
    pub version: u32,
    #[serde(rename = "headsetId")]
    pub headset_id: String,
    #[serde(rename = "headsetModel")]
    pub headset_model: String,
    pub displays: u32,
    pub capabilities: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Welcome {
    #[serde(rename = "companionId")]
    pub companion_id: String,
    #[serde(rename = "hostOs")]
    pub host_os: HostOs,
    #[serde(rename = "hostOsVersion")]
    pub host_os_version: String,
    #[serde(rename = "maxDisplays")]
    pub max_displays: u32,
    #[serde(rename = "osCaps")]
    pub os_caps: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
#[serde(rename_all = "lowercase")]
pub enum HostOs {
    Macos,
    Windows,
    Linux,
    Freebsd,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct VideoFrame {
    #[serde(rename = "displayId")]
    pub display_id: u32,
    pub ts: u64,
    pub keyframe: bool,
    pub codec: String,
    /// Base64-encoded NAL units for the control-channel preview path.
    /// Real video traffic moves to WebRTC in M4 — see ROADMAP.md.
    pub payload: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum InputEvent {
    MouseMove {
        #[serde(rename = "displayId")]
        display_id: u32,
        x: i32,
        y: i32,
    },
    MouseButton {
        button: MouseButton,
        down: bool,
    },
    MouseWheel {
        dx: i32,
        dy: i32,
    },
    Key {
        scancode: u32,
        down: bool,
    },
    Touch {
        #[serde(rename = "pointerId")]
        pointer_id: u32,
        x: i32,
        y: i32,
        phase: TouchPhase,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
#[serde(rename_all = "snake_case")]
pub enum MouseButton {
    Left,
    Right,
    Middle,
    Back,
    Forward,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
#[serde(rename_all = "snake_case")]
pub enum TouchPhase {
    Down,
    Move,
    Up,
    Cancel,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct DisplayReady {
    #[serde(rename = "displayId")]
    pub display_id: u32,
    pub width: u32,
    pub height: u32,
    #[serde(rename = "refreshHz")]
    pub refresh_hz: u32,
    pub scale: f32,
    #[serde(rename = "colorSpace")]
    pub color_space: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct DisplayGone {
    #[serde(rename = "displayId")]
    pub display_id: u32,
    pub reason: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct CursorEvent {
    #[serde(rename = "displayId")]
    pub display_id: u32,
    pub x: i32,
    pub y: i32,
    pub visible: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct ProtoError {
    pub code: String,
    pub detail: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Goodbye {
    pub reason: String,
}

/// Maximum number of displays the current implementation supports per headset.
/// This is a cap enforced on both ends; it is intentionally lower than the
/// theoretical limit of `u32::MAX` so bugs don't spiral.
pub const MAX_DISPLAYS: u32 = 16;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hello_roundtrips_as_json() {
        let hello = Frame::Hello(Hello {
            version: DEFAULT_PROTOCOL_VERSION,
            headset_id: "test-id".into(),
            headset_model: "Quest 2".into(),
            displays: 2,
            capabilities: vec!["h264".into(), "opus".into()],
        });
        let s = serde_json::to_string(&hello).unwrap();
        assert!(s.contains("\"op\":\"HELLO\""));
        assert!(s.contains("\"displays\":2"));
        let back: Frame = serde_json::from_str(&s).unwrap();
        assert_eq!(hello, back);
    }

    #[test]
    fn input_mouse_move_roundtrips() {
        let ev = Frame::Input(InputEvent::MouseMove {
            display_id: 0,
            x: 100,
            y: 200,
        });
        let s = serde_json::to_string(&ev).unwrap();
        assert!(s.contains("\"kind\":\"mouse_move\""));
        assert!(s.contains("\"displayId\":0"));
        let back: Frame = serde_json::from_str(&s).unwrap();
        assert_eq!(ev, back);
    }

    #[test]
    fn welcome_roundtrips() {
        let welcome = Frame::Welcome(Welcome {
            companion_id: "c-1".into(),
            host_os: HostOs::Macos,
            host_os_version: "14.6".into(),
            max_displays: 4,
            os_caps: vec!["mouse".into(), "keyboard".into()],
        });
        let s = serde_json::to_string(&welcome).unwrap();
        assert!(s.contains("\"hostOs\":\"macos\""));
        let back: Frame = serde_json::from_str(&s).unwrap();
        assert_eq!(welcome, back);
    }

    #[test]
    fn unknown_op_fails_to_deserialize() {
        let bad = r#"{"op":"PINEAPPLE","x":1}"#;
        let r: Result<Frame, _> = serde_json::from_str(bad);
        assert!(r.is_err());
    }

    // Compile-time range checks on protocol constants. `const _:` bindings
    // let the compiler enforce them without tripping clippy's
    // `assertions_on_constants` lint on runtime `assert!` calls.
    const _: () = assert!(DEFAULT_PORT > 1024 && DEFAULT_PORT < 49152);
    const _: () = assert!(MAX_DISPLAYS >= 4 && MAX_DISPLAYS <= 64);
}
