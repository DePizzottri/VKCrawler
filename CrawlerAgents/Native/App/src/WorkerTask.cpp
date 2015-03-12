#include <WorkerTask.h>

#include <Poco/AutoPtr.h>
#include <Poco/Util/Application.h>
#include <Poco/NumberFormatter.h>

#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPMessage.h>

#include <TaskNotifications.h>

#include <Poco/JSON/Parser.h>
#include <PluginsCache.h>

WorkerTask::WorkerTask(Poco::NotificationQueue& queue, int n):
	Task("WorkerTask " + Poco::NumberFormatter::format(n)),
	m_jobQueue(queue)
{
}

void WorkerTask::runTask()
{
	auto& app = Poco::Util::Application::instance();

	const std::string serverHost = app.config().getString("server.host");
	const Poco::UInt16 serverPort = app.config().getUInt("server.port");

	poco_information(app.logger(), name() + " started");

	Poco::AutoPtr<Poco::Notification> pNot(m_jobQueue.waitDequeueNotification());
	while (true)
	{
		StopNotification* sjob = dynamic_cast<StopNotification*> (pNot.get());

		if (sjob != nullptr)
			break;

		CrawlJobNotification* cjob = dynamic_cast<CrawlJobNotification*> (pNot.get());

		//got new job
		Poco::JSON::Parser parser;

		try
		{
			auto v = parser.parse(cjob->get());

			Poco::DynamicAny result = parser.result();

			auto obj = result.extract<Poco::JSON::Object::Ptr>();

			auto type = obj->get("type").extract<std::string>();

			auto& pc = PluginsCache::instance();

			if (!pc.get(type)->isValid(obj))
				throw Poco::RuntimeException("Task is not valid for plugin " + pc.get(type)->getName());

			auto ans = pc.get(type)->process(obj);

			//send back

			using namespace Poco::Net;
			HTTPClientSession session(serverHost, serverPort);

			HTTPRequest req(HTTPRequest::HTTP_POST, "/putTask", HTTPMessage::HTTP_1_1);

			//query parameters
			//req.add("", "");

			auto& reqStream = session.sendRequest(req);
			Poco::JSON::Stringifier::stringify(ans, reqStream);
		}
		catch (Poco::Exception& e)
		{
			poco_error(app.logger(), "Error process task: " + e.displayText());
		}

		pNot = m_jobQueue.waitDequeueNotification();
	}

	poco_information(app.logger(), name() + " finished");
}