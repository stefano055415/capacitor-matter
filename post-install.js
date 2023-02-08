`use strict`;

const {
  execSync
} = require(`child_process`);

execSync(`git clone https://github.com/falconeta/matter-android-libs.git --branch 0.0.1  android/third_party/connectedhomeip/libs`);
