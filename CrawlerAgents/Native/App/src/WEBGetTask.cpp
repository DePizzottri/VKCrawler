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
			continue;
		//request for a task
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
	}

	poco_information(app.logger(), "WEB Get task finished");
}
