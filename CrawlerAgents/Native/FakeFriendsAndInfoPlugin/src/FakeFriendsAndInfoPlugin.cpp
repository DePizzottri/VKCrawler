#include "FakeFriendsAndInfoPlugin.h"

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

#include <random>

POCO_BEGIN_MANIFEST(AbstractPlugin)
    POCO_EXPORT_CLASS(FakeFriendsAndInfoPlugin)
POCO_END_MANIFEST

void pocoInitializeLibrary()
{
    std::cout << "FakeFriendsAndInfoPlugin initializing" << std::endl;
}
void pocoUninitializeLibrary()
{
    std::cout << "FakeFriendsAndInfoPlugin uninitializing" << std::endl;
}

std::string FakeFriendsAndInfoPlugin::name() const
{
    return "FakeFriendsAndInfoPlugin";
}

std::string FakeFriendsAndInfoPlugin::type() const
{
    return "friends_list";
}

Poco::UInt16 FakeFriendsAndInfoPlugin::version() const
{
    return 1;
}

bool FakeFriendsAndInfoPlugin::doValidate(Poco::JSON::Object::Ptr const& obj) const
{
    return true;
}

Poco::JSON::Object::Ptr generate_birthday()
{
    Poco::JSON::Object::Ptr bdate(new Poco::JSON::Object);

    bdate->set("day", 13);
    bdate->set("month", 1);
    bdate->set("year", 1966);

    return bdate;
}

std::string generate_string(int len = 20)
{
    static const char alphanum[] =
        "0123456789"
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        "abcdefghijklmnopqrstuvwxyz";

    std::string s;
    for (int i = 0; i < len; ++i) {
        s += alphanum[rand() % (sizeof(alphanum) - 1)];
    }

    return s;
}

Poco::JSON::Array::Ptr generate_friends_list(int uid)
{
    auto& app = Poco::Util::Application::instance();
    using namespace std;
    using namespace Poco;
    using namespace Poco::Net;

    Poco::JSON::Array::Ptr ret(new Poco::JSON::Array);
    //const int count = 5000;
    //int offset = 0;
    //for (int i = 0; i < 2; ++i)
    //{
    //HTTPClientSession session("api.vk.com", 80);

    //URI uri("/method/friends.get");
    //uri.addQueryParameter("user_id", Poco::NumberFormatter::format(uid));
    //uri.addQueryParameter("fields", "city");
    //uri.addQueryParameter("offset", Poco::NumberFormatter::format(offset));
    //uri.addQueryParameter("count", Poco::NumberFormatter::format(count));

    //HTTPRequest req(HTTPRequest::HTTP_GET, uri.toString(), HTTPMessage::HTTP_1_1);
    //session.sendRequest(req);

    //HTTPResponse resp;
    //auto& respStream = session.receiveResponse(resp);

    //poco_information_f1(app.logger(), "friends.get response status: %d", (int)resp.getStatus());

    using namespace Poco::JSON;

    ////parse response
    //auto response = Poco::JSON::Parser().parse(respStream).extract<Object::Ptr>();

    //if (response->has("error"))
    //{
    //    throw Poco::Exception("Error in friends.get request " + uri.toString());
    //}

    //process response
    //auto usersArray = response->get("response").extract<Array::Ptr>();

    //if (usersArray->size() == 0)
    //    break;
    //offset += count;
    std::random_device rd;
    std::mt19937_64 gen(rd());
    std::uniform_int_distribution<> dis(1, 1000000000l);

    const int friends_num = 500;
    for (int i = 0; i < friends_num; ++i)
    {
        try
        {
            Object::Ptr friends(new Poco::JSON::Object);
            //auto userObj = user.extract<Object::Ptr>();
            //if (!userObj->has("uid"))
            //{
            //  poco_warning(app.logger(), "No uid field");
            //  continue;
            //}
            //else
            //{
            //  auto fuid = userObj->get("uid").extract<Poco::Int32>();
                long long fuid = dis(gen);
                friends->set("uid", fuid);
            //}

            //if (!userObj->has("city"))
            //{
                //poco_warning(app.logger(), "No city field");
                friends->set("city", 2);
            //}
            //else
            //{
            //  auto city = userObj->get("city").extract<int>();
            //  friends->set("city", city);
            //}

            ret->add(friends);
        }
        catch (Poco::Exception & e)
        {
            poco_warning(app.logger(), "Error parsing friends.get object: " + e.displayText());
        }
    }
    //}

    return ret;
}


Poco::JSON::Object::Ptr FakeFriendsAndInfoPlugin::doProcess(Poco::JSON::Object::Ptr const& obj) const
{
    using namespace std;
    using namespace Poco;

    auto& app = Poco::Util::Application::instance();

    //extract task parameters and data
    const string type = obj->get("type").extract<string>();
    int dummyTzd = 0;
    //const Timestamp createDate = DateTimeParser::parse(DateTimeFormat::ISO8601_FRAC_FORMAT, obj->get("createDate").extract<string>(), dummyTzd).timestamp();
    //const Timestamp lastUseDate = DateTimeParser::parse(DateTimeFormat::ISO8601_FRAC_FORMAT, obj->get("lastUseDate").extract<string>(), dummyTzd).timestamp();

    auto friendsIDs = obj->get("data").extract<Poco::JSON::Array::Ptr>();

    //https://api.vk.com/method/friends.get?user_id=$uid&fields=city

    //Poco::JSON::Object::Ptr taskStatistics(new Poco::JSON::Object);

    //taskStatistics->set("type", type);
    //taskStatistics->set("createDate", Poco::DateTimeFormatter::format(createDate, Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
    //taskStatistics->set("lastUseDate", Poco::DateTimeFormatter::format(lastUseDate, Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
    Poco::JSON::Array::Ptr friends(new Poco::JSON::Array);

    //make requests
    for (size_t i = 0; i < friendsIDs->size(); ++i)
    {
        Poco::JSON::Object::Ptr friends_raw(new Poco::JSON::Object);
        friends_raw->set("uid", friendsIDs->getElement<int>(i));
        try {
            friends_raw->set("friends", generate_friends_list(friendsIDs->getElement<int>(i)));
            //auto userInfo = getUserInfo(friendsIDs->getElement<int>(i));
            friends_raw->set("firstName", generate_string());
            friends_raw->set("lastName", generate_string());
            friends_raw->set("city", 2);
            //if (userInfo->has("bdate"))
            //{
            //    try
            //    {
            //        friends_raw->set("birthday", parseBirthday(userInfo->get("bdate").extract<std::string>()));
            //    }
            //    catch (Poco::Exception const& e)
            //    {
            //        poco_warning(app.logger(), "Error set birthday " + e.displayText());
            //        friends_raw->set("birthday", "null");
            //    }
            //}
            //else
            //{
            //}

            friends_raw->set("birthday", generate_birthday());

            friends_raw->set("interests", generate_string(80));

            friends_raw->set("sex", 1);
        }
        catch (Poco::Exception const& e)
        {
            poco_warning(app.logger(), "Collect info " + e.displayText());
            continue;
        }

        //friends_raw->set("birthday", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));
        friends_raw->set("processDate", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));

        friends->add(friends_raw);
    }

    //taskStatistics->set("processDate", Poco::DateTimeFormatter::format(Timestamp(), Poco::DateTimeFormat::ISO8601_FRAC_FORMAT));

    Poco::JSON::Object::Ptr ret(new Poco::JSON::Object);
    ret->set("type", type);
    ret->set("data", friends);

    return ret;
}
