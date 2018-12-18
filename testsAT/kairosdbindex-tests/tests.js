var 
  moment = require('moment'),
  kdb = require('kairosdb'),
  client = kdb.init('kairosdb', 8080, {"debug": false});

async function getVersion() {
    return new Promise(function(resolve, reject) {
      
      client.version(function(err, version){
        if (err) {
          reject(err);
        }
        resolve(version);
      });
    
    });
}

async function getData(data) {
  return new Promise(function(resolve, reject) {

    let start = new Date().getMilliseconds();
    client.query(data, function (err, result) {
      if (err) {
        reject(err);
      }
      let finish = new Date().getMilliseconds();
      console.log(" getData() took: " + (finish - start) + "ms");
      resolve(result);
    });
  
  });
}

(async () => {
  
  var metric = "demo_data";
  var data = {
    "metrics": [
      {
        "name": metric,
        "tags": {
          "host": [
            "demo_server_0"
          ]
        },
        "aggregators": [
            {
              "name": "sum",
              "sampling": {
                "value": "1",
                "unit": "days"
              }
            }
        ]
      }
    ]
  };

  let version = await getVersion();
  if(version.version) {
    console.log("connected!  version found: " + version.version);
  }
  else {
    console.log("unable to connect, check errors and try again.");
    process.exit(1);
  }
  
  // couple helper dates
  let startOfLastMonth = moment().subtract(1, 'months').startOf('month');
  let endOfLastMonth   = moment().subtract(1, 'months').endOf('month');
  let startOfYesterday = moment().subtract(1, 'days').startOf('day');
  let endOfYesterday   = moment().subtract(1, 'days').endOf('day');

  data.start_absolute = startOfLastMonth.format('x');
  data.end_absolute = endOfLastMonth.format('x');
  let dataFromNoPlugin = await getData(data);
  console.log(JSON.stringify(dataFromNoPlugin));

  // update query to add cassandra-lucene plugin
  data.metrics[0].plugins = [
    { name: "cassandra-lucene" }
  ];
  let dataFromPlugin = await getData(data);
  console.log(JSON.stringify(dataFromPlugin));

  if(!dataFromPlugin) {
    console.log("failed getting data from plugin!");
  }
  else {
    console.log("using the following query: " + JSON.stringify(data) + ", with and without the 'cassandra-lucene' plugin, the responses are the same: " + (JSON.stringify(dataFromNoPlugin) ===  JSON.stringify(dataFromPlugin)));
  }

  // now remove all tags and see if they are the same
  delete data.metrics[0].tags;

  
})();
