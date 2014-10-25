/*
    WSH and browser crawler
*/

function print(message)
{
    var msg = new Date() + " " + message
    try
    {
        WScript.Echo(msg);
    }
    catch(e)
    {
        console.log(msg)
    }
}

function get_xmlhttp()
{
    var xmlhttp;
    try {
        xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
        try {
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        } catch (E) {
            xmlhttp = false;
        }
    }
    
    if (!xmlhttp && typeof XMLHttpRequest!='undefined') 
    {
        xmlhttp = new XMLHttpRequest();
    }

    return xmlhttp;
}

function http_get(URL, params)
{
    xmlhttp = get_xmlhttp();
    xmlhttp.open('GET', URL, false);
    xmlhttp.send(params);
    if(xmlhttp.status == 200)
    {
        return xmlhttp.responseText;
    }
    else
    {
        throw {message: "GET status code: " + xmlhttp.status};
    }
}

function http_post(URL, data)
{
    xmlhttp = get_xmlhttp();
    xmlhttp.open('POST', URL, false);
    xmlhttp.setRequestHeader("Content-type","application/x-www-form-urlencoded");
    xmlhttp.send(data);

    if(xmlhttp.readyState == 4 && xmlhttp.status == 200) {
        return xmlhttp.responseText;
    }
    else
    {
        throw {message: "POST status code: " + xmlhttp.status};
    }
}

var host = "localhost";
var port = "27080";
var database = "VK_test";
var collection = "tasks";
var result_collection = "raw_data"

function get_tasks()
{
    var rawTasks = http_get("http://" + host + ":" + port + "/" + database + "/" + collection + "/_find", "limit=1");
    var tasks = eval("(" + rawTasks +")");
    if(tasks.ok != 1)
        throw {message:"Wrong answer from tasks", data : rawTasks};
    return tasks.results
}

function run_tasks(tasks)
{
    var ret = []
    for (var i in tasks)
    {
        try
        {
            print("New task started");
            for (var j in tasks[i].data)
            {
                var url = tasks[i].data[j].URL;
                print("URI: " + url);
                var result = http_get(url);
                ret.push(result);
            }
        }
        catch(e)
        {
            ret.push({"error": e.message});
        }
        if((i+1) % 10 == 0)
        {
            print("More 10 tasks processed");
        }
    }

    return ret;
}

function resultsToString(arr)
{
    var res = "";
    for(var i in arr)
    {
        if(arr[i].error == null)            
            res += ',{"data":"' + arr[i].replace(new RegExp('"', 'g'), "\\\"") +'"}';
        else
            res += ',{"error":"' + arr[i].message +'"}';
    }    

    return "[" + res.substring(1) + "]"
}

function arrayToString(array)
{
    var res = "";
    for(var i in array)
    {
        res += "," + array[i]
    }    

    return "[" + res.substring(1) + "]"
}

function send_results(task_ids)
{
    var uri = "http://" + host + ":" + port + "/" + database + "/" + result_collection + "/" + "_insert"
    var param = 'docs=' + resultsToString(results)
    return http_post(uri, param)
}

function objIdToString(obj)
{
  return '{"$oid":"' + obj['$oid'] + '"}' 
}

function getTasksIds(tasks)
{
    var ret = [];
    for (var i in tasks)
    {
        ret.push(objIdToString(tasks[i]._id))
    }    
    return ret;
}

function update_task_count(tasks)
{
    var uri = "http://" + host + ":" + port + "/" + database + "/" + collection + "/" + "_update"
    var param = 'criteria={"_id":{"$in":' + tasks + '}}&newobj={"$inc":{"usecount":1}}&multi=true&safe=true'
    return http_post(uri, param)
}

print("Started");

var tasks = get_tasks();
print("Got " + tasks.length + " new tasks");
print("Update cout result: " + update_task_count(arrayToString(getTasksIds(tasks))));
var results = run_tasks(tasks);

try
{
    print("Send result: " + send_results(results));
}
catch(e)
{
    print("Send error " + e.message);
}

print("Finished");
