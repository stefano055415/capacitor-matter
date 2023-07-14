`use strict`;

const fs = require('fs');

const google = require('@googleapis/drive');

const drive = google.drive('v2');

const decompress = require('decompress');

var dest = fs.createWriteStream("libs.zip");
drive.files.get(
  { fileId: '1370KAEWwKX6TCX7aWTtpD0aldhk1uXBI', key: "AIzaSyB9IB-sqbYwCA-FKfo20a0_eJXQOkW8SFw", alt: 'media' },
  { responseType: "stream" },
  (err, stream) => {
    if (err) {
      console.log(err);
      return;
    }
    stream.data
      .on("end", () => {
        decompress("libs.zip", "android/third_party/connectedhomeip/libs")
          .then((files) => {
            fs.unlinkSync('libs.zip')
          })
          .catch((error) => {
            console.log(error);
          });

        console.log("Done.")
      })
      .on("error", (err) => {
        console.log(err);
        return process.exit();
      })
      .pipe(dest);
  }
);

