#include <WEBGetTask.h>

#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPMessage.h>

#include <Poco/Util/Application.h>

#include <TaskNotifications.h>

#include <Poco/JSON/Parser.h>

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

	poco_information(app.logger(), "WEB Get task started");
	while (!sleep(delaySeconds))
	{
		//request for a task
		HTTPClientSession session(serverHost, serverPort);

		HTTPRequest req(HTTPRequest::HTTP_GET, "/getTask", HTTPMessage::HTTP_1_1);

		//query parameters
		//req.add("", "");

		session.sendRequest(req);

		HTTPResponse resp;
		auto& respStream = session.receiveResponse(resp);

		poco_information_f1(app.logger(), "Task GET response status: %d", (int)resp.getStatus());

		//parse response
		Poco::JSON::Parser parser;

		try
		{
			auto v = parser.parse(respStream);

			Poco::DynamicAny result = parser.result();

			auto obj = result.extract<Poco::JSON::Object::Ptr>();

			auto urls = obj->get("data").extract<Poco::JSON::Array::Ptr>();

			for (int i = 0; i < urls->size(); ++i)
			{
				//create jobs
				m_jobQueue.enqueueNotification(
					new CrawlJobNotification(urls->getElement<std::string>(i))
					);
			}
		}
		catch (Poco::Exception& e)
		{
			poco_error(app.logger(), "Error parse task: " + e.displayText());
		}
	}

	poco_information(app.logger(), "WEB Get task finished");
}