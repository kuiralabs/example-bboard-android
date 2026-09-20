// Witness implementations for bboard.compact.
//
// The contract declares `witness localSecretKey(): Bytes<32>` (bboard.compact line 39),
// and the generated constructor resolves it when the Contract is built — so deploying
// without this module fails before any circuit runs, with "does not contain a
// function-valued field named localSecretKey".
//
// A witness returns `[nextPrivateState, value]`. The compiled contract checks the value
// is a Uint8Array of exactly 32 bytes and rejects anything else.
//
// This is a TEST FIXTURE. The key is fixed so a localnet deploy is reproducible across
// runs and machines; a real dApp derives its secret from the wallet and never hard-codes
// one. Nothing here should be copied into an application.
const FIXTURE_SECRET_KEY = Uint8Array.from(
  Array.from({ length: 32 }, (_, i) => (i + 1) & 0xff),
);

export const witnesses = {
  // Prefer a secret the caller already put in private state, so a host that manages its
  // own key keeps working; fall back to the fixture key for a bare localnet deploy.
  localSecretKey: (context) => {
    const privateState = context?.privateState ?? {};
    const existing = privateState.secretKey;
    const isBytes32 =
      existing instanceof Uint8Array && existing.length === 32;

    if (isBytes32) {
      return [privateState, existing];
    }
    return [{ ...privateState, secretKey: FIXTURE_SECRET_KEY }, FIXTURE_SECRET_KEY];
  },
};
