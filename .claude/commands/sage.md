## Sagemaker 트리거 API 개발

목표

사용자의 요청을 받을 POSt API 컨트롤러 작성 api/admin/sagemakertraining
비즈니스 로직 : Aws sdk 를 이용하여 지정된 계정에 SagemakerTrainigJob 생성을 요청해야됨.
요청에 필요한 spec은 job을 호출하는 github와 동일한 경로에 sagemaker.json 으로 정의되어있음

아래는 git 에 올라간  sagemaker.json의 내용이다.

{
"jobId": "training-final-004",
"jobType": "training",

"awsRegion": "ap-northeast-2",
"roleArn": "arn:aws:iam::390403881443:role/service-role/AmazonSageMaker-ExecutionRole-20260430T143070",

"trainingImageUri": "390403881443.dkr.ecr.ap-northeast-2.amazonaws.com/capstone/ecr:training",

"instanceType": "ml.g4dn.xlarge",
"instanceCount": 1,

"useSpotInstance": true,
"maxRuntimeSeconds": 3600,
"maxWaitSeconds": 7200,

"volumeSizeGb": 50,

"s3Bucket": "capstone-gachon",
"datasetS3Uri": "s3://capstone-gachon/dataset/dataset_new.csv",

"modelVersion": "training-final-004",
"s3ModelPrefix": "models",

"environment": {
"PYTHONUNBUFFERED": "1"
},

"hyperParameters": {
"epochs": "10",
"batch_size": "32"
}
}


