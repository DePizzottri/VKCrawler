#include <WorkerTask.h>

#include <Poco/AutoPtr.h>
#include <Poco/Util/Application.h>
#include <Poco/NumberFormatter.h>

#include <TaskNotifications.h>

WorkerTask::WorkerTask(Poco::NotificationQueue& queue, int n):
	Task("WorkerTask " + Poco::NumberFormatter::format(n)),
	m_jobQueue(queue)
{
}

void WorkerTask::runTask()
{
	auto& app = Poco::Util::Application::instance();

	poco_information(app.logger(), name() + " started");

	Poco::AutoPtr<Poco::Notification> pNot(m_jobQueue.waitDequeueNotification());
	while (true)
	{
		StopNotification* sjob = dynamic_cast<StopNotification*> (pNot.get());

		if (sjob != nullptr)
			break;

		CrawlJobNotification* cjob = dynamic_cast<CrawlJobNotification*> (pNot.get());

		//got new job
		process(cjob->get());

		pNot = m_jobQueue.waitDequeueNotification();
	}

	poco_information(app.logger(), name() + " finished");
}