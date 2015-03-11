#include "stdafx.h"

#include "FriendsListCrawlPlugin.h"

#include <Poco/ClassLibrary.h>

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

Poco::JSON::Object::Ptr FriendsListCrawlPlugin::doProcess(Poco::JSON::Object::Ptr const& obj) const
{
	return Poco::JSON::Object::Ptr();
}