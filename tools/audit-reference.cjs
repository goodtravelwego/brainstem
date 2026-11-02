// Read-only audit of public static data; never reads browser sessions or credentials.
const fs = require('node:fs');
const vm = require('node:vm');
const crypto = require('node:crypto');
function moduleSource(file) {
  const text=fs.readFileSync(file,'utf8').replace(/^\uFEFF/,'');
  const line=text.split(/\r?\n/).find(line=>line.startsWith('const MODSRC='));
  const literal=line.slice('const MODSRC='.length).trim().replace(/;$/,'');
  return JSON.parse(literal.replace(/\\!/g,'!')).pbm;
}
function definitions(source) {
  const block=source.slice(source.indexOf('const PTS='),source.indexOf('const CATS='));
  const info=source.slice(source.indexOf('const PREF='),source.indexOf('function aboutHTML'));
  const context=Object.create(null);
  vm.runInNewContext(block+info+'\nresult=JSON.stringify({points:PTS,xy:XY,regions:REG,programs:PRESETS.filter(p=>!p.off),info:PINFO,references:PREF,evidence:EVL});',context,{timeout:1000,contextCodeGeneration:{strings:false,wasm:false}});
  return JSON.parse(context.result);
}
function canonical(value) { if(Array.isArray(value))return value.map(canonical);if(value&&typeof value==='object')return Object.fromEntries(Object.keys(value).sort().map(key=>[key,canonical(value[key])]));return value; }
const source=moduleSource('reference/audit-app-2026-11-02.html');
const fresh=definitions(source);
const current=JSON.parse(fs.readFileSync('app/src/main/assets/programs.json','utf8'));
const differences=Object.keys(fresh).filter(key=>JSON.stringify(canonical(fresh[key]))!==JSON.stringify(canonical(current[key])));
const report={reference:'https://brainstem-monitor.netlify.app/app/',date:'2026-11-02',presetCount:fresh.programs.length,stepCount:fresh.programs.reduce((n,p)=>n+p.steps.length,0),matches: differences.length===0,differences,pbmSourceChanged:source!==fs.readFileSync('reference/pbm.html','utf8'),publicPbmSha256:crypto.createHash('sha256').update(source).digest('hex')};
fs.writeFileSync('docs/reference-data-audit.json',JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
if(differences.length)process.exitCode=1;
