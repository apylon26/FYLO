#!/usr/bin/env python3
"""Sign a development APK with FYLO's private backup; refuse other keys."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

FINGERPRINT = '44ff54c7c6a79b7a385ba7f4eca096e40a329edd5180baf0a3618d81bd1f9b4d'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--backup', type=Path, required=True)
    parser.add_argument('--apksigner-jar', type=Path, required=True)
    parser.add_argument('--input', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.input.resolve() == args.output.resolve():
        parser.error('Input and output must be different files')
    with tempfile.TemporaryDirectory(prefix='fylo-sign-') as directory:
        directory = Path(directory)
        with zipfile.ZipFile(args.backup) as backup:
            key = directory / 'fylo.jks'
            key.write_bytes(backup.read('fylo.jks'))
            key.chmod(0o600)
            config = json.loads(backup.read('signing.json'))
        env = dict(os.environ, FYLO_SIGNING_PASSWORD=config['password'])
        staged = directory / 'signed.apk'
        command = ['java', '-jar', str(args.apksigner_jar)]
        subprocess.run(command + ['sign', '--ks', str(key), '--ks-key-alias', config['alias'],
                                 '--ks-pass', 'env:FYLO_SIGNING_PASSWORD',
                                 '--key-pass', 'env:FYLO_SIGNING_PASSWORD',
                                 '--out', str(staged), str(args.input)], env=env, check=True)
        verified = subprocess.run(command + ['verify', '--verbose', '--print-certs', str(staged)],
                                  capture_output=True, text=True, check=True)
        fingerprints = re.findall(r'certificate SHA-256 digest: ([a-fA-F0-9]+)', verified.stdout)
        if fingerprints != [FINGERPRINT]:
            raise RuntimeError('Unexpected signing certificate; APK not delivered')
        # Only install a successfully verified APK into the requested output path.
        args.output.write_bytes(staged.read_bytes())
        print('APK signature verified; stable FYLO certificate matches.')


if __name__ == '__main__':
    main()
