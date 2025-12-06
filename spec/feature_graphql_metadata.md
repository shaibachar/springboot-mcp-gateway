# Feature: Enriched GraphQL Tool Metadata

## Overview
GraphQL tools now advertise richer argument metadata to help clients construct accurate payloads without guessing types or nullability.

## Key Changes
- Each GraphQL argument schema now includes `graphqlType`, `javaType`, and `nullable` flags alongside the existing JSON schema `type`.
- Required detection continues to use GraphQL annotations; `nullable` is derived as the inverse of required.
- REST schemas also expose `javaType` for parity.

## Acceptance Criteria
- `/mcp/tools` responses for GraphQL tools list `graphqlType` and `javaType` inside argument schemas.
- Required GraphQL arguments show `nullable=false`; optional arguments show `nullable=true`.
- REST parameters expose `javaType` without altering prior schema type hints.
