# Context

Publication records are currently signed with `HmacSHA256` and verified by the server before install. That is adequate for the MVP but couples repository and runtime trust to a shared secret and does not model key rotation, key revocation, independent verification, or rich supply-chain provenance.

This entry groups signing, provenance, and publication policy because these features define whether a repository assessment is trustworthy enough to become an installable artifact.

