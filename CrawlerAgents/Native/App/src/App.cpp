
#include <Poco/TaskManager.h>
#include <Poco/Timespan.h>

#include <Poco/Util/ServerApplication.h>
#include <Poco/URI.h>

#include <Poco/DirectoryIterator.h>
#include <Poco/SharedLibrary.h>

#include <Plugin.h>

#include <map>

using namespace Poco;
using namespace Poco::Util;
using namespace std;

//class SimpleCrawlerTask: public CrawlerTask {
//public:
//    SimpleCrawlerTask(NotificationQueue& queue, int n) :
//        CrawlerTask(queue, n)
//    {}
//protected:
//    void process(std::string const& URL) {
//        using namespace Poco::Net;
//
//        Poco::URI uri(URL);
//
//        HTTPClientSession session(uri.getHost(), uri.getPort());
//
//        HTTPRequest req(HTTPRequest::HTTP_GET, uri.getQuery(), HTTPMessage::HTTP_1_1);
//
//        //query parameters
//        //req.add("", "");
//
//        session.sendRequest(req);
//
//        HTTPResponse resp;
//        auto& respStream = session.receiveResponse(resp);
//    }
//};

typedef std::map<std::string, AbstractPlugin*> PluginsMap;
PluginsMap plugins;

class App: public Poco::Util::ServerApplication {
	AbstractPlugin::PluginLoader m_loader;	

protected:
	void initialize(Application & self) {
        loadConfiguration();

		//load plugins
		for (Poco::DirectoryIterator d = Poco::DirectoryIterator(self.config().getString("application.dir")); d != Poco::DirectoryIterator(); ++d)
		{
			auto path = Poco::Path(d->path());
			if (path.getExtension() == "vcpl")
			{
				try
				{
					m_loader.loadLibrary(d->path());

					AbstractPlugin* plug = m_loader.create(path.getBaseName());					

					plugins.insert(std::make_pair(plug->getType(), plug));

					m_loader.classFor(path.getBaseName()).autoDelete(plug);

					poco_information(logger(), "Plugin " + path.getFileName() + " loaded");
				}
				catch (Poco::Exception const& e)
				{
					poco_warning(logger(), "Plugin " + path.getBaseName() + " loading error: " + e.displayText());
				}
			}
		}

        ServerApplication::initialize(self);
    }

    void uninitialize() {
        ServerApplication::uninitialize();
    }

    int main(const std::vector < std::string > & args) {
        
        poco_information(logger(), "Started");
        waitForTerminationRequest();
        poco_information(logger(), "Stopped");

		return Application::EXIT_OK;
    }
};

POCO_SERVER_MAIN(App)