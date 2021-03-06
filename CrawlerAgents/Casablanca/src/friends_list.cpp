// REST_SDK.cpp : Defines the entry point for the console application.
//

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

#include <signal.h>
#include <stdlib.h>
#include <stdio.h>
#include <atomic>

atomic_bool interrupt = false;

void sig_int_handler(int s) {
    cout << "Finishing..." << endl;
    interrupt = true;
}

decltype(auto) go(string_t const& addr) {
    ucout << "Requesting task from server..." << endl;
    auto vkclient = make_shared<http_client>(U("http://api.vk.com/"));
    auto client = make_shared<http_client>(U("http://")+ addr +U("/"));

    // Build request URI and start the request.
    uri_builder builder(U("/getTask"));
    builder.append_query(U("version"), U("1"));
    builder.append_query(U("types"), U("friends_list"));
    auto requestTask = client->request(methods::GET, builder.to_string())
        // Handle response headers arriving.
        .then([=](http_response response) {
        // Write response body into the file.
            return response.extract_json();
        })
        .then([=](json::value js) {
            if (!js.has_field(U("data"))) {
                ucout << "No data field in response!" << endl;
                ucout << js << endl;
                throw runtime_error("No data field in resopse from WEB Interface");
            }

            ucout << "Requesting task from VK..." << endl;
            auto flTasks = vector<pplx::task<json::value>>{};
            for (auto& idobj : js[U("data")].as_array()) {
                ucout << "Requesting " << idobj[U("id")].as_integer() << endl;
                const int count = 5000;
                int offset = 0;
                auto tasks = vector<pplx::task<vector<json::value>>>{};
                for (int i = 0; i < 2; ++i) {
                    uri_builder builder(U("/method/friends.get"));
                    builder.append_query(U("user_id"), idobj[U("id")]);
                    builder.append_query(U("fields"), "city");
                    builder.append_query(U("offset"), offset);
                    builder.append_query(U("count"), count);

                    auto requestForId =
                    vkclient->request(methods::GET, builder.to_string())
                    .then([uid = idobj[U("id")]](http_response response) {
                        if (response.status_code() != web::http::status_codes::OK) {
                            ucout << uid.serialize() + U(" Failed request with status ") + response.reason_phrase() << endl;
                            throw runtime_error("failed request to VK");
                        }
                        return response.extract_json();
                    })
                    .then([uid = idobj[U("id")]](json::value frlstjs) {
                        ucout << U("Get ") + uid.serialize() + U(" OK") << endl;
                        auto resp = frlstjs[U("response")];
                        auto res = vector < json::value >{};
                        if (!resp.is_array()) {
                            ucout << "Response from " << uid << " not an array" << endl;
                            ucout << resp.serialize() << endl;
                            return res;
                        }
                        for (auto man : resp.as_array()) {
                            auto obj = json::value::object();
                            obj[U("uid")] = man[U("uid")];

                            if (man.has_field(U("city")))
                                obj[U("city")] = man[U("city")];
                            else
                                obj[U("city")] = json::value::number(-1);
                            res.push_back(obj);
                        }
                        return res;
                    });
                    tasks.push_back(requestForId);
                    offset += count;
                } //for 1..2

                auto flTask = pplx::when_all(tasks.begin(), tasks.end())
                    .then([uid = idobj[U("id")]](vector<json::value> res) { //Seq[UserIdWithCity]
                    auto fl = json::value::object();
                    fl[U("uid")] = uid;
                    fl[U("friends")] = json::value::array(res);

                    return fl;
                });
                flTasks.push_back(flTask);
            } //for by id

            return pplx::when_all(flTasks.begin(), flTasks.end())
                .then([=](vector<json::value> fls) {
                    auto ret = json::value::object();
                    ret[U("type")] = json::value::string(U("friends_list"));
                    ret[U("data")] = json::value::array(fls);

                    //uri_builder builder(U("/postTask"));

                    //wcout << ret[U("data")] << endl;

                    //ucout << "Making POST" << endl;
                    //return std::tie(postClient->request(methods::POST, builder.to_string(), ret), ret);
                    return ret;
                });
        }); //get task

    return requestTask;
}

pplx::task<http_response> post(json::value val, string_t  addr);

pplx::task<http_response> postAux(http_response resp, json::value val, string_t addr) {
    return resp.extract_utf16string().then([=](string_t respBody) {
        if(respBody != U("Ok")) {
            ucout << U("POST failed :") << respBody << endl;
            return post(val, addr);
        }
        else
        {
            ucout << U("POST Ok") << endl;
            if (interrupt.load() == true)
                exit(0);
            return pplx::task<http_response>{[resp] {return resp; }};
        }
    });
}

pplx::task<http_response> post(json::value val, string_t addr) {
    auto postClient = make_shared<http_client>(U("http://") + addr + U("/"));
    uri_builder builder(U("/postTask"));
    return postClient->request(methods::POST, builder.to_string(), val).then(
        [val, addr](http_response resp) {
            return postAux(resp, val, addr);
    });
}

int main(int argc, char* argv[])
{
    signal(SIGINT, sig_int_handler);

    if (argc != 2) {
        std::cout << "Usage: Crawler_FL host:port";
        return EXIT_FAILURE;
    }

    auto addr = utility::conversions::to_string_t(argv[1]);

    for (;;)
        try
        {
            go(addr)
            .then([addr](json::value val) {
                ucout << "Posting collected data to server..." << endl;
                return post(val, addr);
            })
            .wait();
        }
        catch (const std::exception &e)
        {
            printf("Error exception:%s\n", e.what());
        }

    return 0;
}
