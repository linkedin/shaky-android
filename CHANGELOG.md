# Change Log

## Version 3.0.5 (2024-03-25)
- Fix shaky crash due to : Software rendering doesn't support hardware bitmaps [#73](https://github.com/linkedin/shaky-android/pull/73)
- Update several build dependencies
- Update Falcon library

## Version 3.0.4 (2020-11-2)
- Bump to AGP7/Gradle7
- Build with JDK 11
- Keep target and build SDK to Java 1.8

## Version 3.0.3 (2020-10-20)
- Rename "master" to "main" [#58](https://github.com/linkedin/shaky-android/pull/58)
- Update seismic library and change to use sensor delay normal [#64](https://github.com/linkedin/shaky-android/pull/64)

## Version 3.0.2 (2020-02-17)
- First Maven Central release [#56](https://github.com/linkedin/shaky-android/pull/56)

## Version 3.0.1 (2020-12-22)
- Fix alert dialog styling by setting "materialAlertDialogTheme" [#51](https://github.com/linkedin/shaky-android/pull/51)

## Version 3.0.0 (2020-12-22)

- Add ability to customize title and message of shaky dialog [#43](https://github.com/linkedin/shaky-android/pull/43)
- Add subcategories for Bug feedback type [#44](https://github.com/linkedin/shaky-android/pull/44)
- Make UI themeable [#47](https://github.com/linkedin/shaky-android/pull/47)
- Bump minSdk to 21 [#47](https://github.com/linkedin/shaky-android/pull/47)

## Version 2.0.3 (2020-04-01)

- Fix calling nullable activity directly in broadcast receiver [#37](https://github.com/linkedin/shaky-android/pull/37)
- Adding the option to customize the send icon [#38](https://github.com/linkedin/shaky-android/pull/38)
- Added callback during the feedback submission flow [#40](https://github.com/linkedin/shaky-android/pull/40)

## Version 2.0.2 (2019-05-14)

- Allow configuring shake sensitivity from the feedback UI [#30](https://github.com/linkedin/shaky-android/pull/30)

## Version 2.0.1 (2018-11-28)

- Fix transitive dependency configuration

## Version 2.0.0 (2018-11-27)

- NOTE: This release did not correctly publish dependency information. Use 2.0.1 instead.
- Migrate to AndroidX libraries

## Version 1.2.0 (2018-11-08)

- Auto-captialize input fields (#17)
- Allow setting the sensitivity of the underlying ShakeDetector (#18)
- Integrate [Falcon](https://github.com/jraska/Falcon) screenshot library to support higher quality screenshots (#26)

## Version 1.1.1 (2017-05-17)

- Fix regression where feedback flow is not triggered after shaking

## Version 1.1.0 (2017-05-16)

- Added API to programmatically trigger feedback flow (#6)
- Fixed left-aligned "OK" button on AlertDialog
- Use sentence casing input method for feedback input EditText

## Version 1.0.3

- Fixed localization issues, added Norwegian

## Version 1.0.2

- Fixed #7 - small image bug
- Added localization

## Version 1.0.0

- Initial release.
