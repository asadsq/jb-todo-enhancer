# Publishing TODO Enhancer to the JetBrains Marketplace

This is the end-to-end guide to get this plugin from source onto the
[JetBrains Marketplace](https://plugins.jetbrains.com/). The CI in
[`.github/workflows/release.yml`](./.github/workflows/release.yml) already automates publishing on
every GitHub Release — you only need the one-time account/secret setup below.

Steps marked **[you]** require your JetBrains/GitHub credentials or browser and must be done by you.
Everything else is already wired up in the repo.

---

## 0. Prerequisites

- The plugin builds and passes checks locally:
  ```sh
  ./gradlew test buildPlugin verifyPlugin
  ```
  The distributable lands in `build/distributions/TODO Enhancer-<version>.zip`.
- Decide the final plugin **ID** — it is **permanent** once published.
  Ours is `com.github.asadsq.jbtodoenhancer` (in `src/main/resources/META-INF/plugin.xml`).

## 1. Create a JetBrains account and accept the agreements **[you]**

1. Sign in / register at <https://plugins.jetbrains.com/>.
2. Read and accept the
   [Marketplace Developer Agreement / Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).

## 2. First upload (manual) to create the listing **[you]**

The **first** version of a plugin must be uploaded through the website (it is also moderated/reviewed
before it goes public):

1. Go to <https://plugins.jetbrains.com/plugin/add>.
2. Upload `build/distributions/TODO Enhancer-<version>.zip`.
3. Fill in category (e.g. *Code tools*), description (auto-filled from `plugin.xml`), and submit.
4. After it is created, note the **numeric plugin ID** shown in the plugin's URL
   (`https://plugins.jetbrains.com/plugin/<NUMBER>-todo-enhancer`).

## 3. Wire the Marketplace ID into the README badges

Replace every `MARKETPLACE_ID` placeholder in [`README.md`](./README.md) with that number so the
Version/Downloads badges resolve. (I can do this for you once you share the number.)

## 4. Generate a Marketplace publish token **[you]**

1. On <https://plugins.jetbrains.com/>, open your profile → **My Tokens**.
2. Create a token (any name), copy it — it is shown only once.
3. In the GitHub repo → **Settings → Secrets and variables → Actions**, add secret
   **`PUBLISH_TOKEN`** = that token.

This maps to `publishing { token = environmentVariable("PUBLISH_TOKEN") }` in `build.gradle.kts`.

## 5. Generate a signing certificate **[you]**

Marketplace requires signed plugins. Generate a key + self-signed certificate (see the
[Plugin Signing docs](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)):

```sh
# 1. Private key (you will be prompted for a password — remember it)
openssl genpkey -aes-256-cbc -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096

# 2. Certificate chain
openssl req -key private.pem -new -x509 -days 3650 -out chain.crt

# 3. (If Gradle can't read the encrypted key directly) convert to PKCS#8
openssl pkcs8 -topk8 -inform PEM -outform PEM -in private.pem -out private_encrypted.pem
```

Add three GitHub Actions secrets:

| Secret                 | Value                                                        |
|------------------------|-------------------------------------------------------------|
| `CERTIFICATE_CHAIN`    | full contents of `chain.crt`                                |
| `PRIVATE_KEY`          | full contents of `private.pem` (or `private_encrypted.pem`) |
| `PRIVATE_KEY_PASSWORD` | the password you chose in step 1                            |

These map to the `signing { … }` block in `build.gradle.kts`. Keep these files **out of git**
(they are ignored via `build/`/local paths — never commit them).

## 6. Automated releases from here on

Once the secrets exist, publishing a new version is just:

1. Move your changes under a new version heading in `CHANGELOG.md` (or leave them in `[Unreleased]`;
   the release workflow patches the changelog automatically).
2. Bump `version` in [`gradle.properties`](./gradle.properties).
3. Create a **GitHub Release** (a draft release is prepared by the Build workflow).

The [`release.yml`](./.github/workflows/release.yml) workflow then runs `patchChangelog`, signs and
runs `publishPlugin`, uploads the signed zip as a release asset, and opens a changelog PR. New
versions typically appear on the Marketplace within minutes (updates are not re-moderated).

---

## Quick checklist

- [ ] JetBrains account + legal agreements accepted **[you]**
- [ ] First manual upload done; numeric Marketplace ID obtained **[you]**
- [ ] `MARKETPLACE_ID` replaced in `README.md`
- [ ] `PUBLISH_TOKEN` secret set **[you]**
- [ ] `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` secrets set **[you]**
- [ ] Subsequent releases via GitHub Releases (automated)
