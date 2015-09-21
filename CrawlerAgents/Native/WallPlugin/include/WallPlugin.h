#ifndef Crawler_WallPlugin_INCLUDED
#define Crawler_WallPlugin_INCLUDED

#include "Plugin.h"

class WallPlugin: public AbstractPlugin
{
protected:
	virtual std::string name() const;
	virtual std::string type() const;
	virtual Poco::UInt16 version() const;

	virtual bool doValidate(Poco::JSON::Object::Ptr const& obj) const;
	virtual Poco::JSON::Object::Ptr doProcess(Poco::JSON::Object::Ptr const& obj) const;
};

#endif Crawler_WallPlugin_INCLUDED
