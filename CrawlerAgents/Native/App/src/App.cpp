
#include <Poco/TaskManager.h>
#include <Poco/Timespan.h>

#include <Poco/Util/ServerApplication.h>
#include <Poco/URI.h>

#include <Poco/DirectoryIterator.h>
#include <Poco/SharedLibrary.h>

#include <Plugin.h>
#include <PluginsCache.h>
#include <TaskNotifications.h>

#include <WEBGetTask.h>
#include <WorkerTask.h>

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


class App: public Poco::Util::ServerApplication {

protected:
	void initialize(Application & self) {
		loadConfiguration("../../config/example.properties");

		ServerApplication::initialize(self);

		auto& pc = PluginsCache::instance();
		//load plugins
		for (Poco::DirectoryIterator d = Poco::DirectoryIterator(self.config().getString("application.dir")); d != Poco::DirectoryIterator(); ++d)
		{
			auto path = Poco::Path(d->path());
			if (path.getExtension() == "vcpl")
			{
				try
				{
					pc.loadLibrary(path);

					poco_information(logger(), "Plugin " + path.getFileName() + " loaded");
				}
				catch (Poco::Exception const& e)
				{
					poco_warning(logger(), "Plugin " + path.getBaseName() + " loading error: " + e.displayText());
				}
			}
		}

		//start working theards
		m_manager.start(new WEBGetTask(m_queue));

#ifdef PERFORMANCE_COUNT
		poco_information(logger().get("perf"), "Perfomance counter start");
#endif

		//3 default worker threads
		const Poco::UInt16 workerNum = config().getUInt("workerNum", 3);
		Poco::ThreadPool::defaultPool().addCapacity(workerNum - m_threadPool.capacity() + 10);
		for (int i = 0; i < workerNum; ++i)
			m_manager.start(new WorkerTask(m_queue, i+1));

    }

    void uninitialize() {
		m_manager.cancelAll();
		
		const Poco::UInt16 workerNum = config().getUInt("workerNum", 3);
		for (int i = 0; i < workerNum; ++i)
			m_queue.enqueueNotification(new StopNotification);

		m_manager.joinAll();

        ServerApplication::uninitialize();
    }

    int main(const std::vector < std::string > & args) {
        
        poco_information(logger(), "Started");
        waitForTerminationRequest();
        poco_information(logger(), "Stopped");

		return Application::EXIT_OK;
    }
private:

	Poco::TaskManager m_manager;
	Poco::ThreadPool  m_threadPool;
	Poco::NotificationQueue m_queue;
};

POCO_SERVER_MAIN(App)