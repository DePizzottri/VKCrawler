#include "stdafx.h"

#include <PluginsCache.h>

Poco::SingletonHolder<PluginsCache> PluginsCache::m_instance{};

PluginsCache& PluginsCache::instance()
{
	return *m_instance.get();
}

void PluginsCache::loadLibrary(Poco::Path const& path)
{
	m_loader.loadLibrary(path.toString());

	AbstractPlugin* plug = m_loader.create(path.getBaseName());

	m_plugins.insert(std::make_pair(plug->getType(), plug));

	m_loader.classFor(path.getBaseName()).autoDelete(plug);
}

AbstractPlugin* PluginsCache::get(std::string const& type) const
{
	auto ret = m_plugins.find(type);

	if (ret == m_plugins.end())
		throw Poco::NotFoundException("Plugin with type " + type);

	return ret->second;
}

std::vector<std::string> PluginsCache::getSupportedTypes() const
{
	std::vector<std::string> ret;
	for (auto& v : m_plugins)
	{
		ret.push_back(v.first);
	}

	return ret;
}

PluginsCache::~PluginsCache()
{
}
