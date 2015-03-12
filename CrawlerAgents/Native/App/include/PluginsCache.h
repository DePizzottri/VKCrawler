#ifndef Crawler_PluginsCache_INCLUDED
#define Crawler_PluginsCache_INCLUDED

#include <Plugin.h>

#include <Poco/Path.h>

class PluginsCache
{
public:
	void loadLibrary(Poco::Path const& path);
	
	AbstractPlugin* get(std::string const& type) const;

	std::vector<std::string> getSupportedTypes() const;

	static PluginsCache& instance();

	PluginsCache() = default;
	PluginsCache(PluginsCache const&) = default;
	~PluginsCache();
private:

	AbstractPlugin::PluginLoader m_loader;

	typedef std::map<std::string, AbstractPlugin*> PluginsMap;
	PluginsMap					m_plugins;

	static Poco::SingletonHolder<PluginsCache> m_instance;
};

#endif //Crawler_PluginsCache_INCLUDED