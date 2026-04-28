import * as fs from 'fs';
import * as path from 'path';

/**
 * Absolute path to the minimal test video file used in upload tests.
 *
 * The file is a real, tiny (< 1 KB) MP4 container so the browser can pick it
 * up via setInputFiles without triggering "invalid file type" validation.
 *
 * To regenerate it:
 *   ffmpeg -f lavfi -i color=c=blue:s=64x64:d=1 -t 1 \
 *          -c:v libx264 -pix_fmt yuv420p \
 *          e2e/fixtures/test-video.mp4
 *
 * The file committed in the repo was created with the command above.
 */
export const TEST_VIDEO_PATH = path.resolve(__dirname, 'test-video.mp4');

/**
 * Verify the fixture file exists and throw a helpful error if it is missing.
 */
export function assertTestVideoExists(): void {
  if (!fs.existsSync(TEST_VIDEO_PATH)) {
    throw new Error(
      `Test video fixture not found at ${TEST_VIDEO_PATH}.\n` +
        'Run the following command to generate it:\n' +
        '  ffmpeg -f lavfi -i color=c=blue:s=64x64:d=1 -t 1 \\\n' +
        '         -c:v libx264 -pix_fmt yuv420p \\\n' +
        '         e2e/fixtures/test-video.mp4',
    );
  }
}
