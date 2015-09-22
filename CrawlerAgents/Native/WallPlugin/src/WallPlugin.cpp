#include "WallPlugin.h"

#include <Poco/ClassLibrary.h>

#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPMessage.h>

#include <Poco/Util/Application.h>
#include <Poco/Logger.h>
#include <Poco/StreamCopier.h>

#include <Poco/URI.h>

#include <Poco/JSON/Parser.h>
#include <Poco/Timezone.h>

#include <Poco/StringTokenizer.h>

#include <cmath>

POCO_BEGIN_MANIFEST(AbstractPlugin)
    POCO_EXPORT_CLASS(WallPlugin)
POCO_END_MANIFEST

void pocoInitializeLibrary()
{
    std::cout << "FriendsListCrawlPlugin initializing" << std::endl;
}
void pocoUninitializeLibrary()
{
    std::cout << "FriendsListCrawlPlugin uninitializing" << std::endl;
}

std::string WallPlugin::name() const
{
    return "FriendsListCrawlPlugin";
}

std::string WallPlugin::type() const
{
    return "wall_posts";
}

Poco::UInt16 WallPlugin::version() const
{
    return 1;
}

bool WallPlugin::doValidate(Poco::JSON::Object::Ptr const& obj) const
{
    return obj->get("type").extract<std::string>() == WallPlugin::type();
}

std::istream& requestWithoutTimeout(Poco::Net::HTTPClientSession & session, std::string const& method, std::string const& uri, std::string const& version) {
	static auto& app = Poco::Util::Application::instance();
	using namespace Poco;
	using namespace Net;
	while (true) {
		try {
			HTTPRequest countReq(method, uri, version);
			session.sendRequest(countReq);

			HTTPResponse cresp;
			auto& crespStream = session.receiveResponse(cresp);

			poco_information_f1(app.logger(), "Count wall.get response status: %d", (int)cresp.getStatus());

			return crespStream;
		}
		catch (Poco::TimeoutException & e) {
		}
	}
}

void getWall(int uid, Poco::JSON::Array::Ptr & result)
{
    auto& app = Poco::Util::Application::instance();
    using namespace std;
    using namespace Poco;
    using namespace Poco::Net;
	using namespace JSON;
    HTTPClientSession session("api.vk.com", 80);

    //http://api.vk.com/method/wall.get.json?owner_id=30542028&offset=114&count=2&v=5.37

    const string APIVersion = "5.37";

    //get wall posts count
    URI curi("/method/wall.get");
    curi.addQueryParameter("owner_id", Poco::NumberFormatter::format(uid));
    curi.addQueryParameter("offset", "0");
    curi.addQueryParameter("count", "1");
    curi.addQueryParameter("v", APIVersion);

	int wallCount = 0;
	try
	{
		auto& crespStream = requestWithoutTimeout(session, HTTPRequest::HTTP_GET, curi.toString(), HTTPMessage::HTTP_1_1);

		using namespace Poco::JSON;

		//parse response
		auto countResponse = Poco::JSON::Parser().parse(crespStream).extract<Object::Ptr>();

		if (countResponse->has("error"))
		{
			throw Poco::Exception("Error in count wall.get request " + curi.toString());
		}

		auto countRespData = countResponse->get("response").extract<Object::Ptr>();
		wallCount = countRespData->get("count").extract<Int32>();
	}
	catch (Poco::Exception & e) 
	{
		poco_error(app.logger(), "Error process ID count: " + e.displayText());
	}

    const int step = 10;
    int offset = 0;

    for (size_t butchNum = 0; butchNum<floor(wallCount/(double)step); ++butchNum) {
		URI uri("/method/wall.get");
		uri.addQueryParameter("owner_id", Poco::NumberFormatter::format(uid));
		uri.addQueryParameter("offset", Poco::NumberFormatter::format(offset));
		uri.addQueryParameter("count", Poco::NumberFormatter::format(step));
		uri.addQueryParameter("v", APIVersion);
		try {

			auto& respStream = requestWithoutTimeout(session, HTTPRequest::HTTP_GET, uri.toString(), HTTPMessage::HTTP_1_1);

			auto response = Poco::JSON::Parser().parse(respStream).extract<Object::Ptr>();

			if (response->has("error"))
			{
				poco_warning(app.logger(), "Error in wall.get request " + uri.toString());
				continue;
			}

			auto respData = response->get("response").extract<Object::Ptr>();

			auto wallRecords = respData->get("items").extract<Array::Ptr>();

			for (auto record : *wallRecords) {
				result->add(record);
			}
		} 
		catch (Poco::Exception & e) 
		{
			poco_error(app.logger(), "Error process ID: " + uri.toString() + "\n" + e.displayText());
		}

        offset+=step;
    }
    //get wall count
}

/*
[
  {post},
  {post},
  {post}
]
*/

Poco::JSON::Object::Ptr WallPlugin::doProcess(Poco::JSON::Object::Ptr const& obj) const
{
    using namespace std;
    using namespace Poco;

    auto& app = Poco::Util::Application::instance();

    //auto friendsIDs = obj->get("ids").extract<Poco::JSON::Array::Ptr>();

    auto crawlIDs = obj->get("data").extract<Poco::JSON::Array::Ptr>();

    Poco::JSON::Array::Ptr ret(new Poco::JSON::Array);

	for (size_t i = 0; i < crawlIDs->size(); ++i)
    {
        Poco::JSON::Object::Ptr (new Poco::JSON::Object);

		auto uid = crawlIDs->getElement<int>(i);

        getWall(uid, ret);
    }

    Poco::JSON::Object::Ptr retObject(new Poco::JSON::Object);

    retObject->set("type", obj->get("type").extract<string>());
    retObject->set("data", ret);

    return retObject;
}