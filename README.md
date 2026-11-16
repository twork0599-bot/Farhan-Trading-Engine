# Farhan Trading Engine

Android-first, signal-only chart analysis companion for Binolla 1-minute charts.

## Current development status

This repository is the starting point for the Android application. The first milestone is a safe Android shell with explicit screen-capture consent and an optional floating signal panel.

## Planned signal states

- **UP** — bullish conditions agree.
- **DOWN** — bearish conditions agree.
- **WAIT** — chart data is unclear, incomplete, or conflicting.

## Safety

- Signal display only. No automated trading, taps, or order placement.
- Screen capture starts only after the user grants Android MediaProjection consent.
- Floating overlay is optional and requires the user's permission.
- No account credentials are collected.
- Signals are not guaranteed and must be paper-tested before any real-money use.

## Planned analysis

EMA, RSI, MACD, Bollinger Bands, Stochastic, ADX, ATR, support/resistance, candle patterns, trend structure, and multi-signal agreement. Features will be enabled only when reliable candle data can be extracted; uncertain readings must return WAIT.

## Build

The Android project and GitHub Actions APK build workflow will be added in the next commits.
