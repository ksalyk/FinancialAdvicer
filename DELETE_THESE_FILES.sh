#!/bin/bash

# Files to delete (reference files that should be removed from project)
# These are example/documentation files and are NOT used by the build system

FILES_TO_DELETE=(
  "1_root_build.gradle.kts"
  "2_commonMain_NetworkClient.kt"
  "2b_commonMain_ApiClient.kt"
  "3_androidMain_AndroidHttpClientEngine.kt"
  "3b_androidMain_InspektorUI.kt"
  "4_iosMain_IosHttpClientEngine.kt"
  "5_androidMain_MainActivity.kt"
)

echo "🧹 FinancialLifeV2 Project Cleanup"
echo "=================================="
echo ""
echo "These reference files will be deleted:"
echo ""

for file in "${FILES_TO_DELETE[@]}"; do
  if [ -f "$file" ]; then
    echo "  ❌ $file ($(wc -l < "$file") lines)"
  fi
done

echo ""
echo "To delete these files, run:"
echo "  rm 1_root_build.gradle.kts"
echo "  rm 2_commonMain_NetworkClient.kt"
echo "  rm 2b_commonMain_ApiClient.kt"
echo "  rm 3_androidMain_AndroidHttpClientEngine.kt"
echo "  rm 3b_androidMain_InspektorUI.kt"
echo "  rm 4_iosMain_IosHttpClientEngine.kt"
echo "  rm 5_androidMain_MainActivity.kt"
echo ""
echo "Or run this command:"
echo "  rm {1_root_build.gradle.kts,2_commonMain_NetworkClient.kt,2b_commonMain_ApiClient.kt,3_androidMain_AndroidHttpClientEngine.kt,3b_androidMain_InspektorUI.kt,4_iosMain_IosHttpClientEngine.kt,5_androidMain_MainActivity.kt}"
