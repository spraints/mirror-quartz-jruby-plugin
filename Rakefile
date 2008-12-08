require 'rake'
require 'java'

task :default => :build_jar

# compile task from http://blog.foemmel.com/jrake/compiling

CLASSES_DIR = 'tmp/classes'

task :build_jar => [ :compile ] do
  jar CLASSES_DIR, 'lib/quartz-rails.jar'
end

task :compile do
  src_dir = 'src/java'
  dest_dir = CLASSES_DIR
  
  FileUtils::mkdir_p(dest_dir) unless File::exist?(dest_dir)

  javac(src_dir, dest_dir)
end

def jar(src_dir, target)
  sh 'jar', 'cf', target, '-C', src_dir, '.'
end

def build_class_path
  FileList['dependencies/*.jar'].map{|f| File.expand_path(f)}.join(';')
end

def javac(src_dir, dest_dir)
  java_files = FileList["#{src_dir}/**/*.java"]
  
  unless java_files.empty?
    sh 'javac', '-target', '1.5', '-cp', build_class_path, '-d', dest_dir, *java_files
  end
end

