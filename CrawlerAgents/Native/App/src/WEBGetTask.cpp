#include <WEBGetTask.h>

#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPMessage.h>
#include <Poco/URI.h>

#include <Poco/Util/Application.h>

#include <TaskNotifications.h>

#include <Poco/StreamCopier.h>
#include <PluginsCache.h>

#ifdef PERFORMANCE_COUNT
#include <atomic>
#include <chrono>
static std::atomic<int>			perf_time = 0;
static std::atomic<int>         perf_count = 0;       
#endif


WEBGetTask::WEBGetTask(Poco::NotificationQueue & queue):
    Poco::Task("WEBGetTask"), m_jobQueue(queue)
{
}

void WEBGetTask::runTask() {
    using namespace Poco::Net;

    auto& app = Util::Application::instance();

    const string serverHost = app.config().getString("server.host");
    const Poco::UInt16 serverPort = app.config().getUInt("server.port");
    const Poco::UInt16 delaySeconds = app.config().getUInt("GetTaskDelay") * 1000; //1000 milliseconds in second
    const Poco::UInt16 maxQueuedTasks = app.config().getUInt("maxQueuedTasks", 0);

    poco_information(app.logger(), "WEB Get task started");
    while (!sleep(delaySeconds))
    {
		if (m_jobQueue.size() > maxQueuedTasks)
		{
#ifdef PERFORMANCE_COUNT
			//poco_warning(app.logger().get("perf"), "Job queue overflow");
#endif
			continue;
		}
        //request for a task
#ifdef PERFORMANCE_COUNT
		auto start = std::chrono::system_clock::now();
#endif
		HTTPClientSession session(serverHost, serverPort);

        URI uri("/getTask");

        //query parameters
        uri.addQueryParameter("version", "1");

        {
            auto types = PluginsCache::instance().getSupportedTypes();
            std::string param;
            for (auto& t: types)
            {
                param += t + ",";
            }
            param = param.substr(0, param.length() - 1);
            uri.addQueryParameter("types", param);
        }

        try
        {
            HTTPRequest req(HTTPRequest::HTTP_GET, uri.toString(), HTTPMessage::HTTP_1_1);
            session.sendRequest(req);

            HTTPResponse resp;
            auto& respStream = session.receiveResponse(resp);

            poco_information_f1(app.logger(), "Task GET response status: %d", (int)resp.getStatus());

            std::string dataBuf;

            Poco::StreamCopier::copyToString(respStream, dataBuf);

            m_jobQueue.enqueueNotification(
                new CrawlJobNotification(dataBuf)
                );
#ifdef PERFORMANCE_COUNT
			auto stop = std::chrono::system_clock::now();
			perf_time += std::chrono::duration_cast<std::chrono::milliseconds>(stop - start).count();
			perf_count++;
			if (perf_count != 0 && perf_count % 10 == 0)
				poco_information_f3(app.logger().get("perf"), "GET: total: %d ms count: %d mean: %.2f ms", perf_time.load(), perf_count.load(), (double)perf_time.load() / perf_count.load());
#endif
        }
        catch (Exception const& e)
        {
            poco_warning(app.logger(), "Error get new task " + e.displayText());
        }
    }

    poco_information(app.logger(), "WEB Get task finished");
}
