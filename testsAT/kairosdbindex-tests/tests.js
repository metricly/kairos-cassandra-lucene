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

async function getData(query) {
  return new Promise(function(resolve, reject) {

    console.log("starting query: " + JSON.stringify(query));
    let start = new Date().getMilliseconds();
    client.query(query, function (err, result) {
      if (err) {
        reject(err);
      }
      let finish = new Date().getMilliseconds();
      console.log("finished query, took: " + (finish - start) + "ms");
      resolve(result);
    });
  
  });
}

async function compareWithPlugin(query, dataFromNoPlugin) {

  // update query to add cassandra-lucene plugin
  query.metrics[0].plugins = [
    { name: "cassandra-lucene" }
  ];
  let dataFromPlugin = await getData(query);

  if(!dataFromPlugin) {
    console.log("failed getting data from plugin!");
  }
  else {
    let result = (JSON.stringify(dataFromNoPlugin) ===  JSON.stringify(dataFromPlugin));
    console.log("responses are the same: " + result );
    if(!result) {
        console.log("");
        console.log("ERROR: ")
        console.log(" dataFromPlugin: " + JSON.stringify(dataFromPlugin));
        console.log(" dataFromNoPlugin: " + JSON.stringify(dataFromNoPlugin));
        console.log("");
    }
  }
}

/**
 * defaultQuery is fixed to start/end date of last month, this should be a reliable fixed window to compare datasets against
 */
function getDefaultQuery() {
  let defaultQuery = {
    "metrics": [
      {
        "name": "demo_data",
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

  defaultQuery.start_absolute = moment().subtract(1, 'months').startOf('month').format('x');
  defaultQuery.end_absolute   = moment().subtract(1, 'months').endOf('month').format('x');

  return defaultQuery;
}

(async () => {
  
  let version = await getVersion();
  if(version.version) {
    console.log("connected!  version found: " + version.version);
  }
  else {
    console.log("unable to connect, check errors and try again.");
    process.exit(1);
  }
  
  /**
   * TEST1: Run the default query
   **/
  let defaultQuery = getDefaultQuery();
  let dataFromNoPlugin = await getData(defaultQuery);
  await compareWithPlugin(defaultQuery, dataFromNoPlugin);

  /**
   * TEST2: remove all tags
   * now remove all tags and see if they are the same
   **/
  //var queryWithNoTags = Object.assign({}, defaultQuery);
  var queryWithNoTags = getDefaultQuery();
  // remove tags
  delete queryWithNoTags.metrics[0].tags;
  dataFromNoPlugin = await getData(queryWithNoTags);
  await compareWithPlugin(queryWithNoTags, dataFromNoPlugin);


})();
