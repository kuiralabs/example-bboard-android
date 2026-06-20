# BBoard — Android (Kuira SDK example)

A shared **on-chain bulletin board** for Midnight, built on the
[Kuira Android SDK](https://kuiralabs.github.io/kuira-sdk-android/). One slot:
anyone connected to the board can **post** a message, and the poster can **take
it down** — each action is a real Compact contract transaction, proved on the
device.

It's the SDK's end-to-end "hello world": Sigil identity + embedded wallet (both
ride the SDK's floating `PanelBar`), then a contract with two circuits
(`post` / `takeDown`) and a lossless typed ledger read for the current message.

## What it shows

- **Identity + Wallet** — the SDK's `PanelBar` in floating mode drops two
  draggable chips over the parchment: a **sigil** chip (one biometric forges a
  passkey-PRF DID + wallet seed, no seed phrase) and a **wallet** chip (NIGHT +
  DUST balance, receive-QR, dust registration, the network picker, and
  settings). Tap a chip to expand its sheet; drag it to dock at a screen edge.
  Settings opens as a Compose popup that renders above the sheet — the neutral,
  monochrome pills theming themselves into the host's brand.
- **Contract** — `bboard.compact` (`contract/src/`), compiled to JS + proving
  keys under `contract/src/managed/bboard`. The `io.github.kuiralabs.contract`
  Gradle plugin syncs it into the app's assets at build (`kuiraContract { … }`
  in `app/build.gradle.kts`).
- **dApp logic** — `BBoardViewModel` drives deploy → `post` → `takeDown`, with a
  staged progress bar (`ContractCallProgressBar`) over the prove/balance/submit
  wait. The board reads its on-chain state via `BBoardRepository.fetchBoardState()`
  on connect, then re-reads on the explicit **refresh** button — a manual,
  pull-to-read flow (no live ledger subscription).

## Run

```
./gradlew :app:installDebug
```

Then forge a sigil, fund the wallet on localnet, deploy a board, and post.

This example pins `io.github.kuiralabs:dapp-ui` and the
`io.github.kuiralabs.contract` plugin, resolved from Maven Central. To point it
at your own passkey domain, set `rpId` in `IdentityConfigModule` and host a
matching `assetlinks.json`.

## License

Apache-2.0 — see [LICENSE](LICENSE).
