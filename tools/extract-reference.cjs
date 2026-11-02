// Extract ONLY the public, static program definitions. No account/session data is read.
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('reference/pbm.html', 'utf8');
const definitions = source.slice(source.indexOf('const PTS='), source.indexOf('const CATS='));
const info = source.slice(source.indexOf('const PREF='), source.indexOf('function aboutHTML'));
const sandbox = Object.create(null);
vm.runInNewContext(definitions + info + '\nresult=JSON.stringify({points:PTS,xy:XY,regions:REG,programs:PRESETS.filter(p=>!p.off),info:PINFO,references:PREF,evidence:EVL});', sandbox, {timeout:1000,contextCodeGeneration:{strings:false,wasm:false}});
fs.writeFileSync('app/src/main/assets/programs.json', sandbox.result);
const shell=fs.readFileSync('reference-app.html','utf8');
const url=shell.match(/const AU_SB=\{url:'([^']+)'/)[1];
const key=shell.match(/const AU_SB=\{url:'[^']+',key:'([^']+)'/)[1];
fs.writeFileSync('app/src/main/assets/backend.json',JSON.stringify({url,key,redirect:'brainstem://auth/callback'}));
console.log('Extracted '+JSON.parse(sandbox.result).programs.length+' public Stimulate programs.');
