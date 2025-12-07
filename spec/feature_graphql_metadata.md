# Enriched GraphQL Tool Metadata

## Purpose
Expose argument shapes clearly so clients can build valid payloads without guessing types or nullability.

## Contract
- Each GraphQL argument schema includes `graphqlType`, `javaType`, `nullable`, and existing JSON schema `type`.
- Required detection reuses GraphQL annotations (`@Argument(required=true|false)`), recognizes `Optional`/`@Nullable` hints, and sets `nullable` as the inverse of required.
- REST parameters also expose `javaType` for parity.

## Acceptance criteria
- `/mcp/tools` responses for GraphQL tools list `graphqlType` and `javaType` per argument.
- Required arguments show `nullable=false`; optional ones show `nullable=true`.
- REST parameters surface `javaType` while keeping previous schema hints intact.
