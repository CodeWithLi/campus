Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@271ce10c] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1355635351 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: SELECT id,category_id,title,original_file_name,file_ext,content_type,file_size,storage_path,process_status,fail_reason,retry_count,embedding_model,chunk_count,parsed_content,processed_at,remark,created_at,updated_at FROM kb_document WHERE id=?
==> Parameters: 1(Long)
<==    Columns: id, category_id, title, original_file_name, file_ext, content_type, file_size, storage_path, process_status, fail_reason, retry_count, embedding_model, chunk_count, parsed_content, processed_at, remark, created_at, updated_at
<==        Row: 1, 1, 广州商学院-学生维普操作手册.docx, 广州商学院-学生维普操作手册.docx, docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document, 1098758, C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx, PENDING, null, 0, text-embedding-v4, 0, <<BLOB>>, null, null, 2026-04-15 13:20:15, 2026-04-15 13:20:15
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@271ce10c]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@55b774dd] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@298628329 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: UPDATE kb_document SET category_id=?, title=?, original_file_name=?, file_ext=?, content_type=?, file_size=?, storage_path=?, process_status=?, retry_count=?, embedding_model=?, chunk_count=?, created_at=?, updated_at=? WHERE id=?
==> Parameters: 1(Long), 广州商学院-学生维普操作手册.docx(String), 广州商学院-学生维普操作手册.docx(String), docx(String), application/vnd.openxmlformats-officedocument.wordprocessingml.document(String), 1098758(Long), C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx(String), PROCESSING(String), 0(Integer), text-embedding-v4(String), 0(Integer), 2026-04-15T13:20:15(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 1(Long)
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@55b774dd]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@623e699c] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1466400453 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: SELECT id,category_id,title,original_file_name,file_ext,content_type,file_size,storage_path,process_status,fail_reason,retry_count,embedding_model,chunk_count,parsed_content,processed_at,remark,created_at,updated_at FROM kb_document ORDER BY id DESC
==> Parameters: 
<==    Columns: id, category_id, title, original_file_name, file_ext, content_type, file_size, storage_path, process_status, fail_reason, retry_count, embedding_model, chunk_count, parsed_content, processed_at, remark, created_at, updated_at
<==        Row: 1, 1, 广州商学院-学生维普操作手册.docx, 广州商学院-学生维普操作手册.docx, docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document, 1098758, C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx, PROCESSING, null, 0, text-embedding-v4, 0, <<BLOB>>, null, null, 2026-04-15 13:20:15, 2026-04-15 13:20:15
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@623e699c]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@6ebf0d12] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1638864920 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: SELECT id,category_id,title,original_file_name,file_ext,content_type,file_size,storage_path,process_status,fail_reason,retry_count,embedding_model,chunk_count,parsed_content,processed_at,remark,created_at,updated_at FROM kb_document WHERE id=?
==> Parameters: 1(Long)
<==    Columns: id, category_id, title, original_file_name, file_ext, content_type, file_size, storage_path, process_status, fail_reason, retry_count, embedding_model, chunk_count, parsed_content, processed_at, remark, created_at, updated_at
<==        Row: 1, 1, 广州商学院-学生维普操作手册.docx, 广州商学院-学生维普操作手册.docx, docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document, 1098758, C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx, PROCESSING, null, 0, text-embedding-v4, 0, <<BLOB>>, null, null, 2026-04-15 13:20:15, 2026-04-15 13:20:15
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@6ebf0d12]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@4e47eb6b] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1476831276 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: UPDATE kb_document SET category_id=?, title=?, original_file_name=?, file_ext=?, content_type=?, file_size=?, storage_path=?, process_status=?, retry_count=?, embedding_model=?, chunk_count=?, created_at=?, updated_at=? WHERE id=?
==> Parameters: 1(Long), 广州商学院-学生维普操作手册.docx(String), 广州商学院-学生维普操作手册.docx(String), docx(String), application/vnd.openxmlformats-officedocument.wordprocessingml.document(String), 1098758(Long), C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx(String), PROCESSING(String), 0(Integer), text-embedding-v4(String), 0(Integer), 2026-04-15T13:20:15(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 1(Long)
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@4e47eb6b]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7dbc9163] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1396198012 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: UPDATE kb_document SET category_id=?, title=?, original_file_name=?, file_ext=?, content_type=?, file_size=?, storage_path=?, process_status=?, retry_count=?, embedding_model=?, chunk_count=?, parsed_content=?, processed_at=?, created_at=?, updated_at=? WHERE id=?
==> Parameters: 1(Long), 广州商学院-学生维普操作手册.docx(String), 广州商学院-学生维普操作手册.docx(String), docx(String), application/vnd.openxmlformats-officedocument.wordprocessingml.document(String), 1098758(Long), C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx(String), SUCCESS(String), 0(Integer), text-embedding-v4(String), 0(Integer), 一、系统登录
★第1步：打开系统登录页面:https://cloud.fanyu.com/organ/lib/gzcc
★第2步：选择登录入口，输入账号密码登录
学生登录账号为学号，初始密码为：Gzsxy123#

首页：登录后进入首页，所有角色可以看到工作流，工作流的作用如图所示：



二、个人信息填写
第一步：点击左侧菜单进入“个人设置——个人信息”，这里可以填写自己的个人信息、电子签名等。（红色“*”为必填项，个人信息没有完善，不能进行其他操作）


	第二步：可根据自己需求在安全中心中进行绑定安全邮箱、绑定手机号、绑定微信、密码修改等操作。



三、系统主流程及操作方法
（一）课题选题
1.指导教师申报课题—教研室主任审核课题—教学院长审核课题—学生选择课题—指导教师审核选题（学生操作对应2）

2.学生选择课题
点击左侧菜单进入“选题管理”—“选题”—“学生选题”页面。
点击表头的“选择课题”按钮，选择对应的课题，点击“确定选题”。
（此条适用指导老师申报课题流程）


（二）过程管理
1.学生查看任务书
点击左侧菜单进入“过程管理”—“任务书”—“查询任务书”页面—点击“查看”。

2.学生提交开题报告
点击左侧菜单进入“过程管理”—“开题报告”—“编辑开题报告”页面。
点击“编辑”按钮，在弹出页面中，编辑相关内容后，点击“确定”。



3.学生提交中期检查
点击左侧菜单进入“过程管理”—“中期检查”—“编辑中期检查”页面。
点击“编辑”按钮，在弹出页面中，上传文件后，点击“确定”。


4.学生提交查看周进展
点击左侧菜单进入“过程管理”—“周进展”—“查询周进展”页面—点击“查看”。

5.学生提交初稿
点击左侧菜单进入“过程管理”—“初稿”—“编辑初稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文初稿，点击“确认”。

6.学生提交定稿
点击左侧菜单进入“过程管理”—“定稿”—“编辑定稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文定稿，点击“确认”。



注：翻译实践报告此流程为外语学院特有，其他学院无需上传

（三）最终稿
1.学生提交最终稿
点击左侧菜单进入“最终稿”—“最终稿”—“编辑最终稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文稿件，点击“确认”。


image7.png

image8.png

image9.png

image10.png

image11.png

image12.png

image13.png

image14.png

image15.png

image16.png

image17.png

image18.png

image1.png

image2.png

image3.png

image4.png

image5.png

image6.png(String), 2026-04-15T13:21:37.776165400(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 1(Long)
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@7dbc9163]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@208ced6b] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1765910608 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: SELECT id,category_id,title,original_file_name,file_ext,content_type,file_size,storage_path,process_status,fail_reason,retry_count,embedding_model,chunk_count,parsed_content,processed_at,remark,created_at,updated_at FROM kb_document ORDER BY id DESC
==> Parameters: 
<==    Columns: id, category_id, title, original_file_name, file_ext, content_type, file_size, storage_path, process_status, fail_reason, retry_count, embedding_model, chunk_count, parsed_content, processed_at, remark, created_at, updated_at
<==        Row: 1, 1, 广州商学院-学生维普操作手册.docx, 广州商学院-学生维普操作手册.docx, docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document, 1098758, C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx, SUCCESS, null, 0, text-embedding-v4, 0, <<BLOB>>, 2026-04-15 13:21:38, null, 2026-04-15 13:20:15, 2026-04-15 13:20:15
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@208ced6b]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@5714de6f] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1241870989 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: SELECT id,category_id,title,original_file_name,file_ext,content_type,file_size,storage_path,process_status,fail_reason,retry_count,embedding_model,chunk_count,parsed_content,processed_at,remark,created_at,updated_at FROM kb_document WHERE id=?
==> Parameters: 1(Long)
<==    Columns: id, category_id, title, original_file_name, file_ext, content_type, file_size, storage_path, process_status, fail_reason, retry_count, embedding_model, chunk_count, parsed_content, processed_at, remark, created_at, updated_at
<==        Row: 1, 1, 广州商学院-学生维普操作手册.docx, 广州商学院-学生维普操作手册.docx, docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document, 1098758, C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx, SUCCESS, null, 0, text-embedding-v4, 0, <<BLOB>>, 2026-04-15 13:21:38, null, 2026-04-15 13:20:15, 2026-04-15 13:20:15
<==      Total: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@5714de6f]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@2bda93c6] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@1514981236 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: UPDATE kb_document SET category_id=?, title=?, original_file_name=?, file_ext=?, content_type=?, file_size=?, storage_path=?, process_status=?, retry_count=?, embedding_model=?, chunk_count=?, parsed_content=?, processed_at=?, created_at=?, updated_at=? WHERE id=?
==> Parameters: 1(Long), 广州商学院-学生维普操作手册.docx(String), 广州商学院-学生维普操作手册.docx(String), docx(String), application/vnd.openxmlformats-officedocument.wordprocessingml.document(String), 1098758(Long), C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx(String), PROCESSING(String), 0(Integer), text-embedding-v4(String), 0(Integer), 一、系统登录
★第1步：打开系统登录页面:https://cloud.fanyu.com/organ/lib/gzcc
★第2步：选择登录入口，输入账号密码登录
学生登录账号为学号，初始密码为：Gzsxy123#

首页：登录后进入首页，所有角色可以看到工作流，工作流的作用如图所示：



二、个人信息填写
第一步：点击左侧菜单进入“个人设置——个人信息”，这里可以填写自己的个人信息、电子签名等。（红色“*”为必填项，个人信息没有完善，不能进行其他操作）


	第二步：可根据自己需求在安全中心中进行绑定安全邮箱、绑定手机号、绑定微信、密码修改等操作。



三、系统主流程及操作方法
（一）课题选题
1.指导教师申报课题—教研室主任审核课题—教学院长审核课题—学生选择课题—指导教师审核选题（学生操作对应2）

2.学生选择课题
点击左侧菜单进入“选题管理”—“选题”—“学生选题”页面。
点击表头的“选择课题”按钮，选择对应的课题，点击“确定选题”。
（此条适用指导老师申报课题流程）


（二）过程管理
1.学生查看任务书
点击左侧菜单进入“过程管理”—“任务书”—“查询任务书”页面—点击“查看”。

2.学生提交开题报告
点击左侧菜单进入“过程管理”—“开题报告”—“编辑开题报告”页面。
点击“编辑”按钮，在弹出页面中，编辑相关内容后，点击“确定”。



3.学生提交中期检查
点击左侧菜单进入“过程管理”—“中期检查”—“编辑中期检查”页面。
点击“编辑”按钮，在弹出页面中，上传文件后，点击“确定”。


4.学生提交查看周进展
点击左侧菜单进入“过程管理”—“周进展”—“查询周进展”页面—点击“查看”。

5.学生提交初稿
点击左侧菜单进入“过程管理”—“初稿”—“编辑初稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文初稿，点击“确认”。

6.学生提交定稿
点击左侧菜单进入“过程管理”—“定稿”—“编辑定稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文定稿，点击“确认”。



注：翻译实践报告此流程为外语学院特有，其他学院无需上传

（三）最终稿
1.学生提交最终稿
点击左侧菜单进入“最终稿”—“最终稿”—“编辑最终稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文稿件，点击“确认”。


image7.png

image8.png

image9.png

image10.png

image11.png

image12.png

image13.png

image14.png

image15.png

image16.png

image17.png

image18.png

image1.png

image2.png

image3.png

image4.png

image5.png

image6.png(String), 2026-04-15T13:21:38(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 1(Long)
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@2bda93c6]
Creating a new SqlSession
SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@53d5c4f6] was not registered for synchronization because synchronization is not active
JDBC Connection [HikariProxyConnection@898549679 wrapping com.mysql.cj.jdbc.ConnectionImpl@3c1c58ba] will not be managed by Spring
==>  Preparing: UPDATE kb_document SET category_id=?, title=?, original_file_name=?, file_ext=?, content_type=?, file_size=?, storage_path=?, process_status=?, fail_reason=?, retry_count=?, embedding_model=?, chunk_count=?, parsed_content=?, processed_at=?, created_at=?, updated_at=? WHERE id=?
==> Parameters: 1(Long), 广州商学院-学生维普操作手册.docx(String), 广州商学院-学生维普操作手册.docx(String), docx(String), application/vnd.openxmlformats-officedocument.wordprocessingml.document(String), 1098758(Long), C:/Users/dongxin/Desktop/race/campus-ai-rag-service/uploads/kb/2026/04/b804624fdf6e418bb3016343ddc271f3.docx(String), FAILED(String), 未配置 DASHSCOPE_API_KEY，无法执行向量化(String), 0(Integer), text-embedding-v4(String), 0(Integer), 一、系统登录
★第1步：打开系统登录页面:https://cloud.fanyu.com/organ/lib/gzcc
★第2步：选择登录入口，输入账号密码登录
学生登录账号为学号，初始密码为：Gzsxy123#

首页：登录后进入首页，所有角色可以看到工作流，工作流的作用如图所示：



二、个人信息填写
第一步：点击左侧菜单进入“个人设置——个人信息”，这里可以填写自己的个人信息、电子签名等。（红色“*”为必填项，个人信息没有完善，不能进行其他操作）


	第二步：可根据自己需求在安全中心中进行绑定安全邮箱、绑定手机号、绑定微信、密码修改等操作。



三、系统主流程及操作方法
（一）课题选题
1.指导教师申报课题—教研室主任审核课题—教学院长审核课题—学生选择课题—指导教师审核选题（学生操作对应2）

2.学生选择课题
点击左侧菜单进入“选题管理”—“选题”—“学生选题”页面。
点击表头的“选择课题”按钮，选择对应的课题，点击“确定选题”。
（此条适用指导老师申报课题流程）


（二）过程管理
1.学生查看任务书
点击左侧菜单进入“过程管理”—“任务书”—“查询任务书”页面—点击“查看”。

2.学生提交开题报告
点击左侧菜单进入“过程管理”—“开题报告”—“编辑开题报告”页面。
点击“编辑”按钮，在弹出页面中，编辑相关内容后，点击“确定”。



3.学生提交中期检查
点击左侧菜单进入“过程管理”—“中期检查”—“编辑中期检查”页面。
点击“编辑”按钮，在弹出页面中，上传文件后，点击“确定”。


4.学生提交查看周进展
点击左侧菜单进入“过程管理”—“周进展”—“查询周进展”页面—点击“查看”。

5.学生提交初稿
点击左侧菜单进入“过程管理”—“初稿”—“编辑初稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文初稿，点击“确认”。

6.学生提交定稿
点击左侧菜单进入“过程管理”—“定稿”—“编辑定稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文定稿，点击“确认”。



注：翻译实践报告此流程为外语学院特有，其他学院无需上传

（三）最终稿
1.学生提交最终稿
点击左侧菜单进入“最终稿”—“最终稿”—“编辑最终稿”页面。
点击“编辑”按钮，在弹出页面中，上传论文稿件，点击“确认”。


image7.png

image8.png

image9.png

image10.png

image11.png

image12.png

image13.png

image14.png

image15.png

image16.png

image17.png

image18.png

image1.png

image2.png

image3.png

image4.png

image5.png

image6.png(String), 2026-04-15T13:21:49.434375700(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 2026-04-15T13:20:15(LocalDateTime), 1(Long)
<==    Updates: 1
Closing non transactional SqlSession [org.apache.ibatis.session.defaults.DefaultSqlSession@53d5c4f6]
2026-04-15T13:21:49.441+08:00  INFO 14124 --- [nio-8888-exec-7] c.a.r.s.e.GlobalExceptionHandler         : BusinessException: code=422, msg=未配置 DASHSCOPE_API_KEY，无法执行向量化
