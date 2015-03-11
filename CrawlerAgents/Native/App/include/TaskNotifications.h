#include <string>

#include <Poco/Notification.h>

class StopNotification : public Poco::Notification {};

class CrawlJobNotification : public Poco::Notification
{
public:
	explicit CrawlJobNotification(std::string const& data) :
		m_data(data)
	{}

	const std::string& get() const {
		return m_data;
	}
private:
	std::string m_data;
};
