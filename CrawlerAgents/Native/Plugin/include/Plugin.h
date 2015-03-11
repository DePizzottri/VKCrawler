#ifndef Crawler_AbstractPlugin_INCLUDED
#define Crawler_AbstractPlugin_INCLUDED

#include <string>
#include <Poco/Types.h>

#include <Poco/JSON/Object.h>

#include <Poco/ClassLoader.h>
#include <Poco/Manifest.h>

class AbstractPlugin
{
public:
	typedef Poco::ClassLoader<AbstractPlugin> PluginLoader;
	typedef Poco::Manifest<AbstractPlugin> PluginManifest;

	AbstractPlugin();
	virtual ~AbstractPlugin();

	bool isValid(Poco::JSON::Object::Ptr const& obj) const;
	
	Poco::JSON::Object::Ptr process(Poco::JSON::Object::Ptr const& obj) const;

	std::string getName() const;
	std::string getType() const;
	Poco::UInt16 getVersion() const;

protected:
	virtual std::string name() const = 0;
	virtual std::string type() const = 0;
	virtual Poco::UInt16 version() const = 0;

	virtual bool doValidate(Poco::JSON::Object::Ptr const& obj) const = 0;
	virtual Poco::JSON::Object::Ptr doProcess(Poco::JSON::Object::Ptr const& obj) const = 0;
};

#endif //Crawler_AbstractPlugin_INCLUDED