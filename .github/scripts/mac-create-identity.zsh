#./sirius_dist/build/tmp/artifacts/sirius-${{ steps.sirius_version.outputs.value }}-macOS-x86-64.pkg
#./sirius_dist/build/tmp/artifacts/sirius-${{ steps.sirius_version.outputs.value }}-macOS-x86-64-headless.pkg
#rm -rf ./*.dmg
#jpackage --name Unattach --app-version $VERSION --description "$DESCRIPTION" --vendor "$VENDOR" --copyright "$COPYRIGHT" \
#    --license-file LICENSE \
#    --type dmg --app-image Unattach.app
# Sign DMG.
#codesign -s "Developer ID Application: Rok Strnisa (73XQUXV944)" --options runtime --entitlements macos.entitlements -vvvv --deep Unattach-$VERSION.dmg
#sign pkg
#/usr/bin/productsign --timestamp --sign '"'"$MACOS_IDENTITY_ID"'"' ./sirius_dist/build/tmp/artifacts/sirius-$VERSION-osx64.pkg  ./sirius_dist/build/tmp/artifacts/sirius-$VERSION-macOS-x86-64.pkg
/usr/bin/productsign --timestamp --sign "$MACOS_IDENTITY_ID" ./sirius_dist/build/tmp/artifacts/sirius-$VERSION-osx64-headless.pkg ./sirius_dist/build/tmp/artifacts/sirius-$VERSION-macOS-x86-64-headless.pkg

xcrun altool --list-providers -u "$MACOS_APPLE_ID" -p "$MACOS_APPLE_ID_PW"

# Upload pkg for verification.
REQUEST_UUID=$(xcrun altool --notarize-app --primary-bundle-id "app.$APP_NAME-$VERSION" -u "$MACOS_APPLE_ID" -p "$MACOS_APPLE_ID_PW" --file ./sirius_dist/build/tmp/artifacts/sirius-$VERSION-macOS-x86-64-headless.pkg | grep RequestUUID | awk '{print $3}')
# Wait for verification to complete.
while xcrun altool --notarization-info "$REQUEST_UUID" -u "$MACOS_APPLE_ID" -p "$MACOS_APPLE_ID_PW" | grep "Status: in progress" > /dev/null; do
  echo "Verification in progress..."
  sleep 30
done
# Attach stamp to the pkg.
xcrun stapler staple Unattach-$VERSION.dmg
# Check APP and pkg.
spctl -vvv --assess --type exec Unattach.app
codesign -vvv --deep --strict Unattach-$VERSION.dmg
codesign -dvv Unattach-$VERSION.dmg