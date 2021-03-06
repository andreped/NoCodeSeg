// fetch badge as html code
//var returned_text = fetchText();

// write badge to disk, to be able to use it in markdown later
//download(returned_text, "./source/html/generate-badge.txt", "text/plain");

//const { default: fetch } = await import('node-fetch.js')
var nf = require("node-fetch");
const { JSDOM } = require("jsdom");
const { default: document } = (new JSDOM(`...`)).window;
var fs = require('fs');

import fetch from "node-fetch";

let url = "https://dataverse.no/api/info/metrics/filedownloads?parentAlias=ntnu";
let response = await fetch(url);
if (response.status === 200) {
    let content = await response.text();
    let pid = "10.18710/TLA01U";
    let output = getBadge(content, pid);
    
    // write result to disk
    fs.appendFile('./test.txt', output, function (err) {
        if (err) return console.log(err);
        console.log('Appended!');
    });
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
