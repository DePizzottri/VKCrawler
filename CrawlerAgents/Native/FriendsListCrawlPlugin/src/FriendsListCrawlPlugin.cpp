#include "stdafx.h"

#include "FriendsListCrawlPlugin.h"

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

POCO_BEGIN_MANIFEST(AbstractPlugin)
	POCO_EXPORT_CLASS(FriendsListCrawlPlugin)
POCO_END_MANIFEST

void pocoInitializeLibrary()
{
	std::cout << "FriendsListCrawlPlugin initializing" << std::endl;
}
void pocoUninitializeLibrary()
{
	std::cout << "FriendsListCrawlPlugin uninitializing" << std::endl;
}

std::string FriendsListCrawlPlugin::name() const
{
	return "FriendsListCrawlPlugin";
}

std::string FriendsListCrawlPlugin::type() const
{
	return "friends_list";
}

Poco::UInt16 FriendsListCrawlPlugin::version() const
{
	return 1;
}

bool FriendsListCrawlPlugin::doValidate(Poco::JSON::Object::Ptr const& obj) const
{
	return true;
}

Poco::JSON::Object::Ptr parseBirthday(std::string const& date)
{
	Poco::JSON::Object::Ptr bdate(new Poco::JSON::Object);

	Poco::StringTokenizer tok(date, ".", Poco::StringTokenizer::TOK_TRIM | Poco::StringTokenizer::TOK_IGNORE_EMPTY);

	if (tok.count() == 3)
	{
		bdate->set("day", Poco::NumberParser::parse(*(tok.begin())));
		bdate->set("month", Poco::NumberParser::parse(*(tok.begin() + 1)));
		bdate->set("year", Poco::NumberParser::parse(*(tok.begin() + 2)));
	}
	else if (tok.count() == 2)
	{
		bdate->set("day", Poco::NumberParser::parse(*(tok.begin())));
		bdate->set("month", Poco::NumberParser::parse(*(tok.begin() + 1)));
	}

	return bdate;
}

Poco::JSON::Array::Ptr friends_get(int uid)
{
	auto& app = Poco::Util::Application::instance();
	using namespace std;
	using namespace Poco;
	using namespace Poco::Net;

	Poco::JSON::Array::Ptr ret(new Poco::JSON::Array);
	const int count = 5000;
	int offset = 0;
	for (int i = 0; i < 2; ++i)
	{
		HTTPClientSession session("api.vk.com", 80);

		URI uri("/method/friends.get");
		uri.addQueryParameter("user_id", Poco::NumberFormatter::format(uid));
		uri.addQueryParameter("fields", "city");
		uri.addQueryParameter("offset", Poco::NumberFormatter::format(offset));
		uri.addQueryParameter("count", Poco::NumberFormatter::format(count));

		HTTPRequest req(HTTPRequest::HTTP_GET, uri.toString(), HTTPMessage::HTTP_1_1);
		session.sendRequest(req);

		HTTPResponse resp;
		auto& respStream = session.receiveResponse(resp);

		poco_information_f1(app.logger(), "friends.get response status: %d", (int)resp.getStatus());

		using namespace Poco::JSON;

		//parse response
		auto response = Poco::JSON::Parser().parse(respStream).extract<Object::Ptr>();

		if (response->has("error"))
		{
			throw Poco::Exception("Error in friends.get request " + uri.toString());
		}

		//process response
		auto usersArray = response->get("response").extract<Array::Ptr>();

		if (usersArray->size() == 0)
			break;
		offset += count;

		for (auto user : *usersArray)
		{
			try
			{
				auto userObj = user.extract<Object::Ptr>();
				auto fuid = userObj->get("uid").extract<Poco::Int32>();
				//auto first_name = userObj->get("first_name").extract<string>();
				//auto last_name = userObj->get("last_name").extract<string>();
				auto city = userObj->get("city").extract<int>();

				Object::Ptr friends(new Poco::JSON::Object);

				friends->set("uid", fuid);
				friends->set("city", city);

				//Array::Ptr friends(new Array);
				//friends->add(fuid);
				//friends->add(city);

				ret->add(friends);
			}
			catch (Poco::Exception & e)
			{
				poco_warning(app.logger(), "Error parsing friends.get object: " + e.displayText());
			}
		}
	}
	
	return ret;
}

Poco::JSON::Object::Ptr getUserInfo(int uid)
{
	auto& app = Poco::Util::Application::instance();
	using namespace std;
	using namespace Poco;
	using namespace Poco::Net;

	HTTPClientSession session("api.vk.com", 80);

	URI uri("/method/users.get");
	uri.addQueryParameter("user_id", Poco::NumberFormatter::format(uid));
	uri.addQueryParameter("fields", "city,bdate");

	HTTPRequest req(HTTPRequest::HTTP_GET, uri.toString(), HTTPMessage::HTTP_1_1);
	session.sendRequest(req);

	HTTPResponse resp;
	auto& respStream = session.receiveResponse(resp);

	poco_information_f1(app.logger(), "users.get response status: %d", (int)resp.getStatus());

	using namespace Poco::JSON;

	//parse response
	auto response = Poco::JSON::Parser().parse(respStream).extract<Object::Ptr>();

	if (response->has("error"))
	{
		throw Poco::Exception("Error in users.get request " + uri.toString());
	}

	//process response
	return response->get("response").extract<Array::Ptr>()->getObject(0);
}

Poco::JSON::Object::Ptr FriendsListCrawlPlugin::doProcess(Poco::JSON::Object::Ptr const& obj) const
{
	using namespace std;
	using namespace Poco;

	auto& app = Poco::Util::Application::instance();

	//extract task parameters and data
	const string type = obj->get("type").extract<string>();
	int dummyTzd = 0;
	const Timestamp createDate = DateTimeParser::parse(DateTimeFormat::ISO8601_FRAC_FORMAT, obj->get("createDate").extract<string>(), dummyTzd).timestamp();
	const Timestamp lastUseDate = DateTimeParser::parse(DateTimeFormat::ISO8601_FRAC_FORMAT, obj->get("lastUseDate").extract<string>(), dummyTzd).timestamp();

	auto friendsIDs = obj->get("data").extract<Poco::JSON::Array::Ptr>();

	//https://api.vk.com/method/friends.get?user_id=$uid&fields=city

	Poco::JSON::Object::Ptr taskStatistics(new Poco::JSON::Object);

	taskStatistics->set("type", type);
	taskStatistics->set("createDate", Poco::DateTimeFormatter::format(createDate, Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
	taskStatistics->set("lastUseDate", Poco::DateTimeFormatter::format(lastUseDate, Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
	Poco::JSON::Array::Ptr friends(new Poco::JSON::Array);

	//make requests
	for (size_t i = 0; i < friendsIDs->size(); ++i)
	{
		Poco::JSON::Object::Ptr friends_raw(new Poco::JSON::Object);
		friends_raw->set("uid", friendsIDs->getElement<int>(i));
		try {
			friends_raw->set("friends", friends_get(friendsIDs->getElement<int>(i)));
			auto userInfo = getUserInfo(friendsIDs->getElement<int>(i));
			friends_raw->set("firstName", userInfo->get("first_name").extract<string>());
			friends_raw->set("lastName", userInfo->get("last_name").extract<string>());
			friends_raw->set("city", userInfo->get("city").extract<int>());
			if (userInfo->has("bdate"))
			{
				try
				{
					friends_raw->set("birthday", parseBirthday(userInfo->get("bdate").extract<std::string>()));
				}
				catch (Poco::Exception const& e)
				{
					poco_warning(app.logger(), "Error set birthday " + e.displayText());
					friends_raw->set("birthday", "null");
				}
			}
			else
			{
				Poco::JSON::Object::Ptr bdate(new Poco::JSON::Object);

				bdate->set("day", 0);
				bdate->set("month", 0);
				bdate->set("year", 0);
				friends_raw->set("birthday", bdate);
				//friends_raw->set("birthday", "null");
			}
		}
		catch (Poco::Exception const& e)
		{
			poco_warning(app.logger(), e.displayText());
			continue;
		}

		//friends_raw->set("birthday", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
		friends_raw->set("processDate", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));

		friends->add(friends_raw);
	}

	taskStatistics->set("processDate", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));

	Poco::JSON::Object::Ptr ret(new Poco::JSON::Object);
	ret->set("task", taskStatistics);
	ret->set("friends", friends);

	return ret;
}