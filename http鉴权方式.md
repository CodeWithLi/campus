http协议通用鉴权URL生成说明
#1. 鉴权说明
开发者需要自行先在控制台创建应用，利用应用中提供的appid，APIKey， APISecret进行鉴权，生成最终请求的鉴权url。鉴权方法见下方1.2。技术咨询可直接提交工单

#1.2 鉴权参数
参数	类型	必须	说明	示例
host	string	是	请求的主机	aichat.xf-yun.com(使用时需替换为实际使用的接口地址）
date	string	是	当前时间戳，采用RFC1123格式，时间偏差需控制在300s内	Fri, 05 May 2023 10:43:39 GMT
POST	string	是	请求方式	/v1.1/chat HTTP/1.1
authorization	string	是	base64编码的签名信息	参考下方生成方式
最终url需要的参数如上，下方以Python为例进行鉴权参数的生成示例，开发者如果使用其它开发语言可以按照相同时间戳和apikey等常量来逐步生成参数和下方示例比对，确保鉴权步骤无误

#1.2.1 date参数生成规则
from datetime import datetime
from time import mktime
from wsgiref.handlers import format_date_time

cur_time = datetime.now()
date = format_date_time(mktime(cur_time.timetuple()))
# 假使生成的date和下方使用的date = Fri, 05 May 2023 10:43:39 GMT

#date参数生成规则
#date必须是UTC+0或GMT时区，RFC1123格式(Wed, 10 Jul 2019 07:35:43 GMT),服务端会对Date进行时钟偏移检査，最大允许300秒的偏差，超出偏差的请求都将被拒绝。
#1.2.2 authorization参数生成规则
1）到控制台获取APIKey 和APISecret参数

2）利用上方的date动态拼接生成字符串tmp，这里以星火url为例，实际使用需要根据具体的请求url替换host和path。

tmp = "host: " + "spark-api.xf-yun.com" + "\n"
tmp += "date: " + date + "\n"
tmp += "POST " + "/v1.1/chat" + " HTTP/1.1"
"""上方拼接生成的tmp字符串如下
host: spark-api.xf-yun.com
date: Fri, 05 May 2023 10:43:39 GMT
POST /v1.1/chat HTTP/1.1
"""
3）利用hmac-sha256算法结合APISecret对上一步的tmp签名，获得签名后的摘要tmp_sha。

import hmac
import hashlib
# 此处假设APISecret = MjlmNzkzNmZkMDQ2OTc0ZDdmNGE2ZTZi 
tmp_sha = hmac.new(self.APISecret.encode('utf-8'), tmp.encode('utf-8'), digestmod=hashlib.sha256).digest()
"""此时生成的tmp_sha结果如下
b'HWt\xcbU\xe5C\x0b/t\xad\x9a\xd1|\xa8\xdf\x02\xa6~\xc5(\xbeV\xaf@\xe1Ob%\x9a\x9d\xfc'
"""
4）将上方的tmp_sha进行base64编码生成signature

import base64
signature = base64.b64encode(tmp_sha).decode(encoding='utf-8')
"""此时生成的结果如下
SFd0y1XlQwsvdK2a0Xyo3wKmfsUovlavQOFPYiWanfw=
"""
5）利用上面生成的signature，拼接下方的字符串生成authorization_origin

# 假设步骤1控制台获取的APIKey=addd2272b6d8b7c8abdd79531420ca3b
authorization_origin = f"api_key='{APIKey}', algorithm='hmac-sha256', headers='host date request-line', signature='{signature}'"
"""此时生成的authorization_origin字符串如下
api_key="addd2272b6d8b7c8abdd79531420ca3b", algorithm="hmac-sha256", headers="host date request-line", signature="SFd0y1XlQwsvdK2a0Xyo3wKmfsUovlavQOFPYiWanfw="
"""
6）最后再将上方的authorization_origin进行base64编码,生成最终的authorization

authorization = base64.b64encode(authorization_origin.encode('utf-8')).decode(encoding='utf-8')
"""此时生成的authorization如下
YXBpX2tleT0iYWRkZDIyNzJiNmQ4YjdjOGFiZGQ3OTUzMTQyMGNhM2IiLCBhbGdvcml0aG09ImhtYWMtc2hhMjU2IiwgaGVhZGVycz0iaG9zdCBkYXRlIHJlcXVlc3QtbGluZSIsIHNpZ25hdHVyZT0iU0ZkMHkxWGxRd3N2ZEsyYTBYeW8zd0ttZnNVb3ZsYXZRT0ZQWWlXYW5mdz0i
"""
#1.2.3 生成最终url
将鉴权参数组合成最终的键值对，并urlencode生成最终的握手url。开发者可先根据上面的步骤一步步进行参数校验，确保生成的参数无误。

from urllib.parse import urlencode

v = {
		"authorization": authorization, # 上方鉴权生成的authorization
        "date": date,  # 步骤1生成的date
    	"host": "spark-api.xf-yun.com" # 请求的主机名，根据具体接口替换
}
url = "https://spark-api.xf-yun.com/v1.1/chat?" + urlencode(v)
"""生成的最终url如下
https://spark-api.xf-yun.com/v1.1/chat?authorization= YXBpX2tleT0iYWRkZDIyNzJiNmQ4YjdjOGFiZGQ3OTUzMTQyMGNhM2IiLCBhbGdvcml0aG09ImhtYWMtc2hhMjU2IiwgaGVhZGVycz0iaG9zdCBkYXRlIHJlcXVlc3QtbGluZSIsIHNpZ25hdHVyZT0iU0ZkMHkxWGxRd3N2ZEsyYTBYeW8zd0ttZnNVb3ZsYXZRT0ZQWWlXYW5mdz0i&date=Fri%2C+05+May+2023+10%3A43%3A39+GMT&host=spark-api.xf-yun.com
"""