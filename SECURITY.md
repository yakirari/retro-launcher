# Security policy

## Support

The project is experimental; no supported-branch list or security-update schedule is promised.
Identify affected versions before claiming a fix. Separately installed emulators have their own upstream lifecycle.

## Private reporting

Do not disclose exploit details, credentials or private data in public issues. Use
[GitHub private vulnerability reporting](https://github.com/yakirari/retro-launcher/security/advisories/new)
when enabled. Maintainer: [@yakirari](https://github.com/yakirari).
If the form is unavailable, ask the maintainer for a private channel without including details.
No public security email or response-time guarantee is asserted. Hosting settings must be enabled
by a repository administrator; adding this document does not activate the form.

Provide version/commit, Android version, impact, minimal reproduction and redacted evidence.
Never attach another application's private data or commercial game files.

## Review areas

- Exported Home/pin-confirmation activities: Android-valid requests and explicit user consent.
- Custom intents: validate publisher/action/URI/extras; never use payloads as arbitrary shell commands.
- Imported configuration/documents: bounded sizes, malformed-input handling and SAF grants.
- Untrusted artwork: download/decode limits, cancellation, atomic writes.
- Shortcut profiles/ownership: do not bypass another Home launcher's pin isolation.
- Logs: titles, paths, volume IDs and extras can be personal data.
- Signing material: keep secrets out of Git, logs, issues and documentation.

Android backup is permitted in the manifest and is device-policy dependent. Do not promise app-private
data never leaves the device. Artwork queries also contact external services.
