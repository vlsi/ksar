// The script generates a random subset of valid jdk, os, timezone, and locale axes.
// You can preview the results by running "npm ci && node matrix.mjs" from this directory.
// See https://github.com/vlsi/github-actions-random-matrix
import { appendFileSync } from 'fs';
import { EOL } from 'os';
import { createGitHubMatrixBuilder } from '@vlsi/github-actions-random-matrix/github';
const { matrix, random } = createGitHubMatrixBuilder();

matrix.addAxis({
  name: 'java_distribution',
  values: [
    'corretto',
    'liberica',
    'microsoft',
    'temurin',
    'zulu',
  ]
});

matrix.addAxis({
  name: 'java_version',
  title: x => 'Java ' + x,
  // Strings allow versions like 18-ea
  values: [
    '17',
    '21',
    '25',
  ]
});

matrix.addAxis({
  name: 'tz',
  title: x => 'tz ' + x,
  values: [
    'America/New_York',
    'Pacific/Chatham',
    'UTC',
  ]
});

matrix.addAxis({
  name: 'os',
  title: x => (x.value || x).replace('-latest', ''),
  values: [
    {value: 'ubuntu-latest', weight: 4},
    {value: 'windows-latest', weight: 1},
    {value: 'macos-latest', weight: 1},
  ]
});

// Test cases when Object#hashCode produces the same results.
// It allows capturing cases when the code uses hashCode as a unique identifier.
matrix.addAxis({
  name: 'hash',
  values: [
    {value: 'regular', title: '', weight: 10},
    {value: 'same', title: 'same hashcode', weight: 1},
  ]
});

matrix.addAxis({
  name: 'locale',
  title: x => x.language + '_' + x.country,
  values: [
    {language: 'de', country: 'DE'},
    {language: 'fr', country: 'FR'},
    {language: 'ru', country: 'RU'},
    {language: 'tr', country: 'TR'},
  ]
});

matrix.setNamePattern(['java_version', 'java_distribution', 'hash', 'os', 'tz', 'locale']);

// Cover every JDK version at least once
matrix.ensureAllAxisValuesCovered('java_version');
// Keep at least one Linux and one Windows job (macOS behaves much like Linux here)
matrix.generateRow({os: {value: 'ubuntu-latest'}});
matrix.generateRow({os: {value: 'windows-latest'}});
// Keep at least one "same hashcode" job
matrix.generateRow({hash: {value: 'same'}});
// Keep at least one Turkish-locale job, since tr_TR case folding breaks naive toLowerCase calls
matrix.generateRow({locale: {language: 'tr'}});

const include = matrix.generateRows(process.env.MATRIX_JOBS || 6);
if (include.length === 0) {
  throw new Error('Matrix list is empty');
}
include.sort((a, b) => a.name.localeCompare(b.name, undefined, {numeric: true}));
include.forEach(v => {
  v.os = v.os.value;
  // Extra JVM arguments passed to the test workers only (not to the Gradle daemon),
  // so locale and JIT-stress options do not disturb Gradle itself.
  // Values are joined with " ::: " because a single argument may contain spaces.
  let testJvmArgs = [];
  // Gradle does not start in tr_TR, so the locale is applied to tests only.
  // See https://github.com/gradle/gradle/issues/17361
  testJvmArgs.push(`-Duser.country=${v.locale.country}`);
  testJvmArgs.push(`-Duser.language=${v.locale.language}`);
  if (v.hash.value === 'same') {
    // "same hashcode" trips up javac, so it is applied to test execution only.
    // See https://github.com/pgjdbc/pgjdbc/pull/2821#issuecomment-1436013284
    testJvmArgs.push('-XX:+UnlockExperimentalVMOptions', '-XX:hashCode=2');
  }
  if (random() > 0.5) {
    // The following options randomise instruction selection in the JIT compiler,
    // so they might reveal missing synchronisation in the code under test.
    v.name += ', stress JIT';
    testJvmArgs.push('-XX:+UnlockDiagnosticVMOptions');
    // Randomise instruction scheduling in GCM and LCM (share/opto/c2_globals.hpp)
    testJvmArgs.push('-XX:+StressGCM', '-XX:+StressLCM');
    // Randomise worklist traversal in IGVN (Java 16+) and CCP (Java 17+).
    // The lowest tested version is 17, so both options always apply.
    testJvmArgs.push('-XX:+StressIGVN', '-XX:+StressCCP');
  }
  v.testExtraJvmArgs = testJvmArgs.join(' ::: ');
  delete v.hash;
});

console.log(include);
let filePath = process.env['GITHUB_OUTPUT'] || '';
if (filePath) {
  appendFileSync(filePath, `matrix<<MATRIX_BODY${EOL}${JSON.stringify({include})}${EOL}MATRIX_BODY${EOL}`, {
    encoding: 'utf8'
  });
}
