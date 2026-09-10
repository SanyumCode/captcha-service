# API Reference

Base URL for local development: `http://127.0.0.1:8080`

The API is unversioned and retains a legacy API-key contract. Use HTTPS through a reverse
proxy outside local development. OAuth 2.0 is not implemented.

## Submit recognition

`POST /captcha/type1`

Header: `Content-Type: application/json`

Request fields:

| Field | Type | Required | Notes |
|---|---|---|---|
| `user` | string | yes | Maximum 100 characters |
| `api_key` | string | yes | Bearer secret, maximum 200 characters |
| `imgBase64` | string | yes | Raw Base64; PNG/JPEG/GIF signature; encoded limit 10 MiB and decoded limit 8 MiB |

```json
{
  "user": "demo-user",
  "api_key": "replace-with-your-api-key",
  "imgBase64": "iVBORw0KGgo="
}
```

Success (`200`):

```json
{"ok":true,"uid":"a1b2c3d4e5"}
```

Domain failure (`200`):

```json
{"ok":false,"cause":"User verification failed. Please check the user and the key."}
```

Malformed content type/JSON returns `400`; unexpected processing errors return `500`.

## Get recognition status

`GET /captcha/get?user={user}&apiKey={key}&uid={uid}`

All values must be URL-encoded. `uid` is alphanumeric and at most 50 characters.

Success (`200`):

```json
{"ok":true,"coded":"A7kP"}
```

Pending (`200`):

```json
{"ok":false,"cause":"The content you requested has not been updated yet. Please try again later."}
```

The query-string API key is a known compatibility limitation because URLs may be retained
by proxies, browser history, or access logs. Disable such logging, always use HTTPS, and
restrict this endpoint to trusted networks. Header-only authentication is on the roadmap.

## Error behavior

`ok=false` causes include missing/illegal fields, Base64 decode failure, unsupported image
signature, failed user verification, unknown UID, pending inference, and generic server
failure. Consumers should branch on `ok`, treat cause text as human-readable (not a stable
machine code), use bounded polling with backoff, and avoid retrying validation failures.

Suggested polling starts near one second, backs off to several seconds with jitter, and
ends at a caller-defined deadline. Retain the UID for support, but never log the API key or
image payload.
