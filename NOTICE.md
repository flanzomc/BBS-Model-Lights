# Upstream attribution

The effects implementation is being ported from ElGatoPro300/BBS-CML, revision
06f0e53172aadb1fd2a201b03226c9cb59f135e5 (MIT, McHorse and ElGatoPro300).
Its license is retained in LICENSE and included in the JAR.
BBS FS is by McHorse and Wemppy. It is an external dependency, not bundled here.

The shader math and block light rules are adapted from CML, with new compatibility
mixins and native FS form properties. This is a separate add-on, not BBS-CML itself.

LambDynamicLights 2.3.4+1.20.4 by LambdAurora (MIT) is bundled unmodified as a nested
JAR for shader-free moving light, including its original notices and nested libraries.
Source: https://github.com/LambdAurora/LambDynamicLights/tree/1.20.4
Artifact SHA-1: b44b10647f170732825b28d8ff44735ae31e3837

Production behavior validation is in progress. Consult TESTING.md and the matching
GitHub Actions run before treating a candidate as tested.
