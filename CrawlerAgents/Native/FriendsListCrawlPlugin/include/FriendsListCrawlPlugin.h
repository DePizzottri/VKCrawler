#ifndef Crawler_FriendsListCrawlPlugin_INCLUDED
#define Crawler_FriendsListCrawlPlugin_INCLUDED

#include "Plugin.h"

class FriendsListCrawlPlugin: public AbstractPlugin
{
protected:
	virtual std::string name() const;
	virtual std::string type() const;
	virtual Poco::UInt16 version() const;

	virtual bool doValidate(Poco::JSON::Object::Ptr const& obj) const;
	virtual Poco::JSON::Object::Ptr doProcess(Poco::JSON::Object::Ptr const& obj) const;
};

#endif //Crawler_FriendsListCrawlPlugin_INCLUDED