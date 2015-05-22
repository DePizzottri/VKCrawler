#include "Plugin.h"

AbstractPlugin::AbstractPlugin()
{

}

AbstractPlugin::~AbstractPlugin()
{

}

bool AbstractPlugin::isValid(Poco::JSON::Object::Ptr const& obj) const
{
    return doValidate(obj);
}

Poco::JSON::Object::Ptr AbstractPlugin::process(Poco::JSON::Object::Ptr const& obj) const
{
    return doProcess(obj);
}

std::string AbstractPlugin::getName() const
{
    return name();
}

std::string AbstractPlugin::getType() const
{
    return type();
}

Poco::UInt16 AbstractPlugin::getVersion() const
{
    return version();
}
