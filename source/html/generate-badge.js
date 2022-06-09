import fetch from 'node-fetch';

// fetch badge as html code
var returned_text = fetchText();

// write badge to disk, to be able to use it in markdown later
download(returned_text, "./source/html/generate-badge.txt", "text/plain");

function download(text, name, type) {
    var a = document.getElementById("a");
    var file = new Blob([text], {type: type});
    a.href = URL.createObjectURL(file);
    a.download = name;
  }

async function fetchText() {
//const { fetch } = await import('node-fetch');  // dynamic import don't work as it should with node-fetch?

let url = "https://dataverse.no/api/info/metrics/filedownloads?parentAlias=ntnu";
let response = await fetch(url);
if (response.status === 200) {
    let content = await response.text();
    let pid = "10.18710/TLA01U";
    let output = getBadge(content, pid);
    var badgeId = document.getElementById("badgeId");
    badgeId.innerHTML += output;
}
}

function getBadge(content, pidToFind) {
var x = content.split("\n");
for (var i = 0; i < x.length; i++) {
    y = x[i].split(",");
    x[i] = y;
}
let total = 0;
x.shift(); // remove header row ("id,pid,count")
for (const row of x) {
    let pid = row[1];
    let count = row[2];
    if (pid.includes(pidToFind)) {
    total += Number(count);
    }
}
return (
    '<a href="https://doi.org/' +
    pidToFind +
    '"><img src="https://img.shields.io/badge/DataverseNO%20downloads-' +
    total +
    '-orange"></a>'
);
}