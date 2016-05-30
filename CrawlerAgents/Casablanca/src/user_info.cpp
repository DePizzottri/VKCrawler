// REST_SDK.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#include <cpprest/http_client.h>
#include <cpprest/filestream.h>

#include <cpprest/http_listener.h>              // HTTP server</span>
#include <cpprest/json.h>                       // JSON library</span>
#include <cpprest/uri.h>                        // URI library</span>
#include <cpprest/ws_client.h>                  // WebSocket client</span>
#include <cpprest/containerstream.h>            // Async streams backed by STL containers</span>
#include <cpprest/interopstream.h>              // Bridges for integrating Async streams with STL and WinRT streams</span>
#include <cpprest/rawptrstream.h>               // Async streams backed by raw pointer to memory</span>
#include <cpprest/producerconsumerstream.h>     // Async streams for producer consumer scenarios</span>
#include <iostream>
#if !(defined(_MSC_VER) && (_MSC_VER >= 1800)) && CPPREST_FORCE_PPLX
#include <pplx/pplx.h>
#endif
#include <pplx/pplxtasks.h>
//#include <concurrent_vector.h>

using namespace utility;                    // Common utilities like string conversions
using namespace web;                        // Common features like URIs.
using namespace web::http;                  // Common HTTP functionality
using namespace web::http::client;          // HTTP client features
using namespace concurrency::streams;       // Asynchronous streams

using namespace web::http::experimental::listener;          // HTTP server
using namespace web::json;                                  // JSON library
using namespace std;

pplx::task<http_response> go() {
    auto vkclient = make_shared<http_client>(U("http://api.vk.com/"));
    auto client = make_shared<http_client>(U("http://192.168.1.4:8081/"));
    auto postClient = make_shared<http_client>(U("http://192.168.1.4:8081/"));

    // Build request URI and start the request.
    uri_builder builder(U("/getTask"));
    builder.append_query(U("version"), U("1"));
    builder.append_query(U("types"), U("user_info"));
    auto requestTask = client->request(methods::GET, builder.to_string())
    // Handle response headers arriving.
    .then([=](http_response response)
    {
        // Write response body into the file.
        return response.extract_json();
    })
    .then([=](json::value js) {
        if (!js.has_field(U("data"))) {
            ucout << "No data field in response!" << endl;
            ucout << js << endl;
            throw exception("No data field in resopse from WEB Interface");
        }

        //auto tasks = make_shared<vector<concurrency::task<json::value>>>();

        auto tasks = make_shared <std::vector<pplx::task<json::value>> > ();
        
        for (auto& idobj : js[U("data")].as_array()) {
            ucout << "Requesting " << idobj[U("id")].as_integer() << endl;

            uri_builder builder(U("/method/users.get"));
            builder.append_query(U("user_ids"), idobj[U("id")]);
            builder.append_query(U("fields"), 
                    U("status,"
                    "sex,"
                    "bdate,"
                    "city,"
                    "country,"
                    "home_town,"
                    "has_photo,"
                    "lists,"
                    "domain,"
                    "contacts,"
                    "site,education,universities,schools,status,last_seen"
                    ",followers_count"
                    //",common_count"
                     ",occupation"
                     ",relatives,relation,personal,connections,exports,wall_comments,activities,interests,music,movies,tv,books,games,about,quotes,career,military"
                    )
                );
            auto requestForId = vkclient->request(methods::GET, builder.to_string())
                .then([uid = idobj[U("id")]](http_response response) {
                if (response.status_code() != web::http::status_codes::OK) {
                    ucout << uid.to_string() + U(" Failed request with status ") + response.reason_phrase() << endl;
                    throw exception("failed request to VK");
                }
                return response.extract_json();
            })
            .then([uid = idobj[U("id")]](json::value ugjs) {
                ucout << U("Get ") + uid.to_string() + U(" OK") << endl;
                auto resp = ugjs[U("response")];
                return *resp.as_array().begin();
            });
            tasks->push_back(requestForId);
        }

        return pplx::when_all(tasks->begin(), tasks->end())
        .then([=](vector<json::value> ans) {
            auto ret = json::value::object();
            ret[U("type")] = json::value::string(U("user_info"));
            ret[U("data")] = json::value::array(ans);

            uri_builder builder(U("/postTask"));

            ucout << "Making POST" << endl;
            return postClient->request(methods::POST, builder.to_string(), ret);
        });
    });

    return requestTask;
}

int main()
{
    for (;;)
    try
    {
        go()
        .then([] (http_response response) {
            return response.extract_utf16string();
        })
        .then([] (string_t respBody) {
            ucout << U("POST:") << respBody << endl;
        })
        .wait();
    }
    catch (const std::exception &e)
    {
        printf("Error exception:%s\n", e.what());
    }

    return 0;
}

